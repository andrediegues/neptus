/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: João Fortuna
 * Dec 5, 2012
 */
package pt.up.fe.dceg.neptus.plugins.ipcam;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JPopupMenu;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.GuiUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;

/**
 * @author jfortuna
 *
 */
@Popup( pos = POSITION.RIGHT, width=400, height=400)
@PluginDescription(name="AirCam Display", author="JFortuna", description="Video display for Ubiquiti IP Camera", icon="pt/up/fe/dceg/neptus/plugins/ipcam/camera.png")
public class AirCamDisplay extends SimpleSubPanel implements ConfigurationListener {

    private static final long serialVersionUID = 1L;

    public enum InterpolationStyle { Nearest_Neighbour, Bilinear, Bicubic }; 

    @NeptusProperty(name="Camera IP", description="The IP address of the camera you want to display")
    public String ip = "10.0.20.209";
    
    @NeptusProperty(name="Milliseconds between connection retries")
    public long millisBetweenTries = 1000;

    protected BufferedImage imageToDisplay = null;
    protected boolean connected = true;
    protected Thread updater = null;
    protected String status = "initializing";

    public AirCamDisplay(ConsoleLayout console) {
        super(console);
        removeAll();
        
        status = "initializing...";
        updater = updaterThread();
        updater.setPriority(Thread.MIN_PRIORITY);
        updater.start();
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add("Reconnect").addActionListener(new ActionListener() {
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            reconnect();
                        }
                    });
                    
                    popup.add("Camera settings").addActionListener(new ActionListener() {
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PropertiesEditor.editProperties(AirCamDisplay.this, getConsole(), true);
                        }
                    });
                    popup.show((Component)e.getSource(), e.getX(), e.getY());
                }
                
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        if (imageToDisplay != null) {
            double factor1 = (getWidth()-1.0) / imageToDisplay.getWidth();
            double factor2 = (getHeight()-1.0) / imageToDisplay.getHeight();
            ((Graphics2D)g).scale(factor1, factor2);
            g.drawImage(imageToDisplay, 0, 0, null);
        }
        else {
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.gray);
            g.drawRect(0, 0, 320, 240);
            g.drawRect(0, 0, 640, 480);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(320, 240);
    }

    public void reconnect() {
        NeptusLog.pub().info("IPCameraDisplay: reconnecting to "+ip+"...");
        connected = false;        
    }

    private Thread updaterThread() {
        return new Thread() {
            
            @Override
            public void run() {

                while(true) {   
                    if (updater != this)
                        return;

                    if (ip == null)
                        break;
                    connected = true;
                    try {
                        URL url = new URL("http://"+ip+"/images/logo.gif");
                        imageToDisplay = ImageIO.read(url);     
                        repaint();
                    }
                    catch (Exception e) {
                        status = "Error: "+e.getMessage();
                        e.printStackTrace();
                        NeptusLog.pub().warn(e);          
                        repaint();
                        connected = false;
                        status = "reconnecting";
                    }
                    finally {

                    }

                    try {
                        Thread.sleep(millisBetweenTries);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().warn(e);
                    }
                }
                NeptusLog.pub().info("<###>Thread exiting...");
            }
        };
    }

    @Override
    public void cleanSubPanel() {
        status = "stopping";
        ip = null;
        connected = false;
        updater.interrupt();
    }

    protected String previousURL = null;
    
    @Override
    public DefaultProperty[] getProperties() {
        previousURL = ip;
        return super.getProperties();
    }
    
    @Override
    public void propertiesChanged() {
        if (!ip.equals(previousURL))
            reconnect();
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        final AirCamDisplay display = new AirCamDisplay(null);
        GuiUtils.testFrame(display, "Camera Display");
    }
}