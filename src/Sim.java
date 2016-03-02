import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.Timer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Sim implements ActionListener {

	private static final double WAYPOINT_RANGE = 5;
	UAVSimPanel uavSimPanel;
	Timer timer;
	int simTickDelay = 10;
	
	double uavSpeed = 3;
	double uavMaxHeadingChange = Math.PI/60;
	Iterator<Point> waypointIterator;
	protected Point currentWaypoint;
	boolean maintainingTurn = false;
	private double turnAmount;
	
    public static void main(String[] args) {
    	final Sim sim = new Sim();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	sim.uavSimPanel  = new UAVSimPanel(800,800);
            	sim.uavSimPanel.uavX = 400; sim.uavSimPanel.uavY = 500;
            	sim.uavSimPanel.waypoints = new ArrayList<Point>();
            	sim.uavSimPanel.waypoints.add(new Point(120,300));
            	sim.uavSimPanel.waypoints.add(new Point(50,720));
            	sim.uavSimPanel.waypoints.add(new Point(720,600));
            	sim.uavSimPanel.waypoints.add(new Point(500,300));
            	sim.waypointIterator = sim.uavSimPanel.waypoints.iterator();
            	sim.currentWaypoint = sim.waypointIterator.next();
                createAndShowGUI(sim.uavSimPanel);
                
              //Set up timer to drive animation events.
                sim.timer = new Timer(sim.simTickDelay, sim);
                sim.timer.setInitialDelay(0);
                sim.timer.start();             
           }
        });
    }

    private static void createAndShowGUI(UAVSimPanel simPanel) {
        System.out.println("Created GUI on EDT? "+
        SwingUtilities.isEventDispatchThread());
        JFrame f = new JFrame("Simulator Demo");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(simPanel);
        f.pack();
        f.setVisible(true);
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		// Simulator 'tick' is performed here
		//System.out.println("Tick.");
		
		//System.out.println("uavHeading = " + uavHeading);
		// update UAV's position based on speed and heading
		uavSimPanel.uavX += uavSpeed * Math.cos(uavSimPanel.uavHeading);
		uavSimPanel.uavY += uavSpeed * Math.sin(uavSimPanel.uavHeading);
		//System.out.println("uavX = " + uavSimPanel.uavX + " uavY = " + uavSimPanel.uavY);
		
		// check to see if it's close to the current waypoint
		if (Math.sqrt(Math.pow(currentWaypoint.x - uavSimPanel.uavX, 2) + Math.pow(currentWaypoint.y - uavSimPanel.uavY, 2)) < WAYPOINT_RANGE) {
			if (waypointIterator.hasNext()) {
				currentWaypoint = waypointIterator.next();
			}
			else {
				// reset to first waypoint
				waypointIterator = uavSimPanel.waypoints.iterator();
			}
		}
		
		// adjust the heading to aim for the waypoint
		// uavHeading = Math.atan2(currentWaypoint.y - uavSimPanel.uavY, currentWaypoint.x - uavSimPanel.uavX);
		double desiredHeading = Math.atan2(currentWaypoint.y - uavSimPanel.uavY, currentWaypoint.x - uavSimPanel.uavX);
		//System.out.println("desiredHeading = " + desiredHeading);
		// special conditions to allow for wrapping around pi and -pi
		if (uavSimPanel.uavHeading > Math.PI/2 && desiredHeading < -Math.PI/2) {
			// add 2pi to desired heading to allow for wrapping
			desiredHeading += 2*Math.PI;
			//System.out.println("corrected desiredHeading = " + desiredHeading);
		}
		if (desiredHeading > Math.PI/2 &&  uavSimPanel.uavHeading < -Math.PI/2) {
			// add 2pi to uav heading to allow for wrapping
			uavSimPanel.uavHeading += 2*Math.PI;
		}
		double headingDifference = desiredHeading - uavSimPanel.uavHeading;
		//System.out.println("headingDifference = " + headingDifference);
		
		// special case if headingDifference is close to pi, we need to always try the same direction until it gets away from pi
		if (maintainingTurn) {
			//System.out.println("Maintaining turn direction.");
			uavSimPanel.uavHeading += turnAmount;
			if (Math.abs(headingDifference) <= Math.PI * 0.85) {
				maintainingTurn = false;
				//System.out.println("No longer maintaining turn direction.");
			}
		} else if (Math.abs(headingDifference) > Math.PI * 0.85) {
			maintainingTurn = true;
			turnAmount = uavMaxHeadingChange * Math.signum(headingDifference);
			//System.out.println("Forcing turn direction of " + turnAmount);
		} else {
			if (Math.abs(headingDifference)  > uavMaxHeadingChange) {
				// only change the heading by the max
				// decide the direction (this is tricky)
				uavSimPanel.uavHeading += uavMaxHeadingChange * Math.signum(headingDifference);
			}
			else {
				uavSimPanel.uavHeading = desiredHeading;
			}

		}
		
		if (uavSimPanel.uavHeading > Math.PI ) uavSimPanel.uavHeading -= 2* Math.PI;
		if (uavSimPanel.uavHeading < -Math.PI ) uavSimPanel.uavHeading += 2* Math.PI;
		
		uavSimPanel.repaint();
	}
}

class UAVSimPanel extends JPanel {
	
	private static final int UAV_SIZE = 4;
	private static final int WAYPOINT_SIZE = 8;
	private static final int ANTENNA_WIDTH = 20;
	public double uavX, uavY;
	public double uavHeading;
	public List<Point> waypoints;

	private int width, height;

    public UAVSimPanel(int width, int height) {
    	this.width = width; this.height = height;
        setBorder(BorderFactory.createLineBorder(Color.black));
    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        // Enable antialiasing 
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        // draw waypoints
        for (Point waypoint : waypoints) {
        	g2d.setColor(Color.red);
			g2d.fillOval(waypoint.x-WAYPOINT_SIZE/2, waypoint.y-WAYPOINT_SIZE/2, WAYPOINT_SIZE, WAYPOINT_SIZE);
		}
        // draw UAV
        g2d.setColor(Color.BLUE);
        g2d.fillOval((int)uavX - UAV_SIZE/2, (int)uavY - UAV_SIZE/2, UAV_SIZE, UAV_SIZE);
        g2d.drawLine((int)uavX, (int)uavY, (int)(uavX+8.0*Math.cos(uavHeading)), (int)(uavY+8.0*Math.sin(uavHeading)));
        
        // draw antenna and its radio range
        g2d.setColor(Color.GRAY);
        g2d.drawRect(width/2 - ANTENNA_WIDTH/4, height/2 - ANTENNA_WIDTH/4, ANTENNA_WIDTH, ANTENNA_WIDTH);
        int range = (int) (Math.min(width, height) * 0.8);
        g2d.drawOval(width/2 - range/2, height/2 - range/2, range, range);
        
        // if UAV is out of radio range, display a ?
        double diffX = uavX - width/2, diffY = uavY - height/2;
        if (Math.sqrt(diffX * diffX + diffY* diffY) > range/2) {
        	FontRenderContext frc = g2d.getFontRenderContext();
        	Font f = new Font("Helvetica",Font.BOLD, 24);
        	String s = new String("?");
        	TextLayout tl = new TextLayout(s, f, frc);
        	Dimension theSize=getSize();
        	g2d.setColor(Color.red);
        	tl.draw(g2d, (int)uavX, (int)uavY);        }
    }  
}
