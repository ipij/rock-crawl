/*
 * DatabaseDemoHostApplication.java
 *
 * Copyright (c) 2008-2009 Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.sunspotworld.demo;

import com.sun.spot.io.j2me.radiogram.*;

import com.sun.spot.peripheral.ota.OTACommandServer;
import com.sun.spot.util.IEEEAddress;
import javax.microedition.io.*;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * This application is the 'on Desktop' portion of the SendDataDemo. This host
 * application collects sensor samples sent by the 'on SPOT' portion running on
 * neighboring SPOTs and graphs them in a window.
 *
 * @author Vipul Gupta modified Ron Goldman
 */
public class SendDataDemoGuiHostApplication {
    // Broadcast port on which we listen for sensor samples

    private static final int HOST_PORT = 70;
    private JTextArea status;
    //At this moment the number is 3 indicating the number of SPOTs
    private long[] addresses = new long[3];
    //4 windows including one for the average value
    private DataWindow[] plots = new DataWindow[4];

    //-----------This method is fine. Don't have to modify this. 
    private void setup() {
        JFrame fr = new JFrame("Send Data Host App");
        status = new JTextArea();
        JScrollPane sp = new JScrollPane(status);
        fr.add(sp);
        fr.setSize(360, 200);
        fr.validate();
        fr.setVisible(true);
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = 0;
            plots[i] = null;
        }
    }
    //-----------------
    //This method tells the app to determine which window to draw the value
    private DataWindow findPlot(long addr) {
        for (int i = 0; i < addresses.length; i++) {
            if (addresses[i] == addr) {
                return plots[i];
            } else if (addresses[i] == 0) {
                String ieee = IEEEAddress.toDottedHex(addr);
                status.append("Received packet from SPOT: " + ieee + "\n");
                addresses[i] = addr;
                plots[i] = new DataWindow(ieee);
                final int ii = i;
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        plots[ii].setVisible(true);
                    }
                });
                return plots[i];
            }            
        }
        return plots[0];
    }

    //This is for receiving data and draw
    private void run() throws Exception {
        RadiogramConnection rCon;
        Radiogram dg;
        
        //create the average window
        plots[3] = new DataWindow("Average Temperature");
        plots[3].setVisible(true);
        DataWindow averagedw = plots[3];
        //-----------------------
        
        //------ This part does not need to modify. dg is the received package.---------//
        try {
            // Open up a server-side broadcast radiogram connection
            // to listen for sensor readings being sent by different SPOTs
            rCon = (RadiogramConnection) Connector.open("radiogram://:" + HOST_PORT);
            dg = (Radiogram) rCon.newDatagram(rCon.getMaximumLength());
        } catch (Exception e) {
            System.err.println("setUp caught " + e.getMessage());
            throw e;
        }

        status.append("Listening...\n");
        //----------------------------------------------//
        // Main data collection loop
        //This part is worth attention
        long addrCheck = 0;
        long count = 0;
        long averageval=0;
        int averagevalint=0;
        long sum=0;
        while (true) {
            try {
                // Read sensor sample received over the radio
                rCon.receive(dg);
                DataWindow dw = findPlot(dg.getAddressAsLong());
                long time = dg.readLong();      // read time of the reading
                int val = (int) dg.readDouble();         // read the sensor value
                dw.addData(time, val);
                
                //a very simple way to calculate the average value. 
                //if the sending address is different than the previous one
                //then calculate the average value
                //Some shortages remain
                if(addrCheck==0)
                {
                    addrCheck=dg.getAddressAsLong();
                    sum=sum+val;
                    averageval=sum/(count+1);
                    averagevalint=(int)averageval;
                    count=(count+1)%3;
                }
                else if(addrCheck==dg.getAddressAsLong())
                {
                    continue;
                }
                else
                {
                    
                    sum+=val;
                    averageval=sum/(count+1);
                    averagevalint=(int)averageval;
                    count=(count+1)%3;
                    if(count==0)sum=0;
                }
                
                averagedw.addData(time, averagevalint);
                        
            } catch (Exception e) {
                System.err.println("Caught " + e + " while reading sensor samples.");
                throw e;
            }
        }
    }
//The main method below does not need to modify
    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) throws Exception {
        // register the application's name with the OTA Command server & start OTA running
        OTACommandServer.start("SendDataDemo-GUI");

        SendDataDemoGuiHostApplication app = new SendDataDemoGuiHostApplication();
        app.setup();
        app.run();
    }
}
