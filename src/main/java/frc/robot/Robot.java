package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.PWMVictorSPX;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.Relay.*;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.cscore.UsbCamera;

public class Robot extends TimedRobot {
  private static final String leftSide = "Default";
  private static final String rightSide = "My Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  UsbCamera Cam0;

  NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");
  NetworkTable lemon = NetworkTableInstance.getDefault().getTable("limelight-lemon");
  NetworkTableEntry tx = table.getEntry("tx"); // angle on x-axis from the crosshairs on the object to origin
  NetworkTableEntry ty = table.getEntry("ty"); // angle on x-axis from the crosshairs on the object to origin
  NetworkTableEntry ta = table.getEntry("ta"); // area of the object
  NetworkTableEntry tv = table.getEntry("tv"); // 1 if have vision 0 if no vision
  NetworkTableEntry tlong = table.getEntry("tlong"); // length of longest side
  NetworkTableEntry tshort = table.getEntry("tshort"); // length of shortest side
  NetworkTableEntry tvert = table.getEntry("tvert"); // vertical distance
  NetworkTableEntry thor = table.getEntry("thor"); // horizontal distance
  NetworkTableEntry getpipe = table.getEntry("getpipe"); // this tells us what "pipeline" we are on, basically different settings for the camera
  NetworkTableEntry ts = table.getEntry("ts"); // skew or rotation of target

  /*NetworkTableEntry tx2 = lemon.getEntry("tx"); // angle on x-axis from the crosshairs on the object to origin
  NetworkTableEntry ty2 = lemon.getEntry("ty"); // angle on x-axis from the crosshairs on the object to origin
  NetworkTableEntry ta2 = lemon.getEntry("ta"); // area of the object
  NetworkTableEntry tv2 = lemon.getEntry("tv"); // 1 if have vision 0 if no vision
  NetworkTableEntry tlong2 = lemon.getEntry("tlong"); // length of longest side
  NetworkTableEntry tshort2 = lemon.getEntry("tshort"); // length of shortest side
  NetworkTableEntry tvert2 = lemon.getEntry("tvert"); // vertical distance
  NetworkTableEntry thor2 = lemon.getEntry("thor"); // horizontal distance
  NetworkTableEntry getpipe2 = lemon.getEntry("getpipe"); // this tells us what "pipeline" we are on, basically different settings for the camera
  NetworkTableEntry ts2 = lemon.getEntry("ts"); // skew or rotation of target */

  Joystick joy = new Joystick(0);

  PWMVictorSPX frontLeft = new PWMVictorSPX(9);
  PWMVictorSPX frontRight = new PWMVictorSPX(7);
  PWMVictorSPX rearLeft = new PWMVictorSPX(8);
  PWMVictorSPX rearRight = new PWMVictorSPX(6);

  VictorSP solo = new VictorSP(2);
  VictorSP lift1 = new VictorSP(4);
  VictorSP lift2 = new VictorSP(5);
  VictorSP intake = new VictorSP(3);

  Relay belt = new Relay(1);

  PowerDistributionPanel PDP = new PowerDistributionPanel(0);

  SpeedControllerGroup left = new SpeedControllerGroup(frontLeft, rearLeft);
  SpeedControllerGroup right = new SpeedControllerGroup(frontRight, rearRight);
  SpeedControllerGroup lift = new SpeedControllerGroup(lift1, lift2);
  SpeedControllerGroup yes = new SpeedControllerGroup(frontLeft, frontRight, rearLeft, rearRight);

  DifferentialDrive buffet = new DifferentialDrive(left, right);

  double pizza;
  double taco;

  double camx;
  double camx2;
  double camy;
  double camarea;
  double targetWidth;
  double vertAngle;

  boolean aligned;
  boolean alignedPickup;
  boolean intakeRun;
  boolean shootRun = false;
  boolean firstTimeThru = true;

  DigitalInput inDown = new DigitalInput(6);
  DigitalInput inUp = new DigitalInput(5);
  DigitalInput liftUp = new DigitalInput(4);
  DigitalInput liftDown = new DigitalInput(3);
  DigitalInput b1 = new DigitalInput(0);
  DigitalInput b2 = new DigitalInput(1);
  DigitalInput b3 = new DigitalInput(2);

  boolean b3Limit = false;
  boolean b3bool;


  int beltDelay;
  int autonomooseDelay = 0;
  int soloCount = 0;

  double P, I, alignIntegral, error, setpoint = 0, piAlign; // alignment P
  double pShooter, errorShooter, setShooter = -2, piShooter; // shooter P


  @Override
  public void robotInit() {
  m_chooser.setDefaultOption("Default Auto", leftSide);
  m_chooser.addOption("My Auto", rightSide);
  SmartDashboard.putData("Auto choices", m_chooser);
  }

  @Override
  public void robotPeriodic() {
    camx = tx.getDouble(0.0);
    camx2 = tx2.getDouble(0.0);
    camy = ty.getDouble(0.0);
    camarea = ta.getDouble(0.0);
    vertAngle = tvert.getDouble(0);
    targetWidth = thor.getDouble(0);

    b3bool = b3.get();
    if(b3bool)
      b3Limit = true;
    

  SmartDashboard.putNumber("LimelightX", (Math.round(100*camx))/100d);
  SmartDashboard.putNumber("LimelightY", (Math.round(100*camy))/100d);
  NetworkTableInstance.getDefault();
  SmartDashboard.putBoolean("Aligned", aligned);
  SmartDashboard.putNumber("PIShooter", piShooter);
  SmartDashboard.putBoolean("b3Limit", b3Limit);
  SmartDashboard.putBoolean("b3", b3bool);


  }

  @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();
    System.out.println("Auto Selected" + m_autoSelected);

  }

  @Override
  public void autonomousPeriodic() {
    final double tv = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tv").getDouble(0);
    switch (m_autoSelected) {
      case rightSide:
        if (autonomooseDelay <= 50) {
          buffet.arcadeDrive(.5, 0);
          ++autonomooseDelay;
        } else if (autonomooseDelay > 50 && tv == 1 && soloCount <= 3) {
          autoShoot();
        } else if (autonomooseDelay > 50 && tv == 0) {
          buffet.arcadeDrive(0, -.15);
        } else {
          buffet.arcadeDrive(0, 0);
        }
            break;
          case leftSide:
          default:
          if (autonomooseDelay <= 50) {
            buffet.arcadeDrive(.5, 0);
            ++autonomooseDelay;
          } else if (autonomooseDelay > 50 && tv == 1 && soloCount <= 3) {
            autoShoot();
          } else if (autonomooseDelay > 50 && tv == 0) {
            buffet.arcadeDrive(0, .15);
          } else {
            buffet.arcadeDrive(0, 0);
          }
          break;
    } 
  }
	/*-=-=-=-=-=-= -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  @Override
  public void teleopPeriodic() {
    pizza = joy.getRawAxis(1);
    taco = joy.getRawAxis(4);
    buffet.arcadeDrive(-pizza, taco);

    Cam0 = CameraServer.getInstance().startAutomaticCapture(0);
		
    lift();
		
    intakeRun();

    manShooter();
    	  
    belt();
  } // teleopperiodic
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  @Override
  public void testPeriodic() {
  }
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  public void autoShoot() {
    // Auto-Aligns to the reflective tape
    aligned = false;
    PIDa();
    PIDs();
    ///////////////////////////////////////////ALIGNMENT
    if (camx > 2) {
      right.set(0);
      left.set(Math.abs(piAlign));
      aligned = false;
    } else if (camx < -2) {
      right.set(-piAlign);
      left.set(0);
    } else if (camx > -2 & camx < 2) {
      aligned = true;
    } 
    ///////////////////////////////////////////ALIGNMENT
    ///////////////////////////////////////////SHOOTER
    if (aligned == true) {
      solo.set(-PIDs());
    } else {
      solo.set(0);
    }                     // MIN DISTANCE IS 6.8\\
    ///////////////////////////////////////////SHOOTER
    ///////////////////////////////////////////BELT
    if (b3bool) {
      beltDelay = 0;
    } else if (aligned == true && beltDelay >= 100) {
      belt.set(Value.kReverse);
    } else if (aligned == true && beltDelay < 100) { // I WANT ENCODER SO WE CAN DO THIS A LOT BETTER AND AUTOMATICALLY
      belt.set(Value.kOff);
      ++beltDelay;
    } else {
      belt.set(Value.kOff);
    } 

    ///////////////////////////////////////////BELT
  }
  /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  public void lift() {
							  // Moves lift up and down
   if (liftUp.get() == false && joy.getRawButton(6)) {
     lift.set(1);
   } else if (liftDown.get() == false && joy.getRawButton(5)) {
     lift.set(-1);
   } else {
     lift.set(0);
   }
 }
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
	public void intakeRun() {
	 // Creates a toggle for intake motors
    if (joy.getRawButtonPressed(1)) {
      intakeRun = !intakeRun;
    }

    // Runs the intake motors
    if (intakeRun == true) {
      intake.set(1);
    } else if (joy.getRawButton(2)) {
      intake.set(-.3);
    } else {
      intake.set(0);
    }
}
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
	public void manShooter() {
	if (joy.getRawButtonPressed(3)) {
      shootRun = !shootRun;
    }
    PIDa();
    if (shootRun == true) {
      solo.set(-PIDs());
    } else {
      solo.set(0);
    }
	}
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  public void belt() {
    final double tv = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tv").getDouble(0);
  // Runs the intake motors
    if (joy.getRawButton(7)) {
      belt.set(Value.kForward);
    } else if (joy.getRawButton(8)) {
      belt.set(Value.kReverse);
    } else if (joy.getRawButton(4) == true && tv == 1) {// Moves us into auto-shooting if button is pressed
      autoShoot();
    } else if (joy.getRawAxis(2) > .5 || joy.getRawAxis(3) > .5) {
      solo.set(.5);
    } else {
      aligned = false;
      belt.set(Value.kOff);
      beltDelay = 0;
    } /*
       * else { belt.set(Value.kOff); }
       */
    /*
     * } else { if (b3.get() == true && joy.getRawButton(4) == false) {
     * belt.set(Value.kOff); } else if (b2.get() == true && b1.get() == false &&
     * joy.getRawButton(4) == false) { belt.set(Value.kOff); } else if
     * (joy.getRawButton(4) == false) { belt.set(Value.kReverse); } }
     */

  }
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  public double PIDa() {
    /*P =.03; // Change higher for less speed and lower for slower speed, change in very very small stages (like .01 small stages)
    error = setpoint - camx;
    if (Math.abs(P*error) < .15) {
      piAlign = .15; // This is the minimun speed for the motors to go, change this and the number in the line right above this (I wouldn't go below .15)
    } else {
      piAlign = P*error;
    }
    return piAlign;//*/
    piAlign = Math.abs(camx)/20+.15+0.0075*(camy+2);
    return 2*piAlign/3;
  }
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  public double PIDs() { 
    pShooter = .02; // Change higher for higher speed and lower for lower speed, also change in very small incraments idk if this is how you spell
    errorShooter = setShooter - camy;  // difference between 
    
    if (pShooter*errorShooter + .75 < .75) {
      piShooter = .75; // Look at PIDa for comments
    } else {
      piShooter = pShooter*errorShooter + .75;
    }

    return Math.abs(piShooter);
  }
}
