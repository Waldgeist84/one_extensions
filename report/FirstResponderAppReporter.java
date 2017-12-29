/* 
 * Copyright 2015 Technische Universität Ilmenau, KN
 * Released under GPLv3. See LICENSE.txt for details. 
 */

package report;

import applications.FirstResponderApplication;
import core.Application;
import core.ApplicationListener;
import core.DTNHost;

/**
 * Reporter for the <code>FirstResponderApplication</code>. Counts the number of reports
 * and commands sent and received. Calculates success probabilities.
 * 
 * @author sk
 */
public class FirstResponderAppReporter extends Report implements ApplicationListener {
	
	private int reportsSent=0;
	private int reportsReceived=0;
	private int reportsForwarded=0;

	private int commandsSent=0;
	private int commandsReceived=0;
	private int commandsForwarded=0;
	
	public void gotEvent(String event, Object params, Application app,
			DTNHost host) {
		// Check that the event is sent by correct application type
		if (!(app instanceof FirstResponderApplication)) return;
		
		// Increment the counters based on the event type
		if (event.equalsIgnoreCase("GotReport")) {
			reportsReceived++;
		}
		if (event.equalsIgnoreCase("SentReport")) {
			reportsSent++;
		}
		if (event.equalsIgnoreCase("ForwardedReport")) {
			reportsForwarded++;
		}
		if (event.equalsIgnoreCase("GotCommand")) {
			commandsReceived++;
		}
		if (event.equalsIgnoreCase("SentCommand")) {
			commandsSent++;
		}
		if (event.equalsIgnoreCase("ForwardedCommand")) {
			commandsForwarded++;
		}
		
	}

	
	@Override
	public void done() {
		write("FirstResponder stats for scenario " + getScenarioName() + 
				"\nsim_time: " + format(getSimTime()));
		double reportProb = 0; // report probability
		double commandProb = 0; // command probability
		double successProb = 0;	// success probability
		
		if (this.reportsSent > 0) {
			reportProb = (1.0 * this.reportsReceived) / (this.reportsSent + this.reportsForwarded);
		}
		if (this.commandsSent > 0) {
			commandProb = (1.0 * this.commandsReceived) / (this.commandsSent + this.commandsForwarded);
		}
		if (this.reportsSent > 0) {
			successProb = (1.0 * (this.commandsReceived + this.reportsReceived) / (this.commandsForwarded + this.commandsSent + this.reportsForwarded + this.reportsSent));
		}
		
		String statsText = "reports sent: " + this.reportsSent + 
			"\nreports forwarded: " + this.reportsForwarded + 
			"\nreports received: " + this.reportsReceived + 
			"\ncommands sent: " + this.commandsSent + 
			"\ncommands forwarded: " + this.commandsForwarded + 
			"\ncommands received: " + this.commandsReceived + 
			"\nreport delivery prob: " + format(reportProb) +
			"\ncommand delivery prob: " + format(commandProb) + 
			"\nsuccess prob: " + format(successProb)
			;
		
		write(statsText);
		super.done();
	}
}
