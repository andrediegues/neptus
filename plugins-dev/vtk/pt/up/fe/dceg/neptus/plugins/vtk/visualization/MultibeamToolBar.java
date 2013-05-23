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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: hfq
 * May 9, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.vtk.Vtk;
import pt.up.fe.dceg.neptus.plugins.vtk.filters.Contours;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.ExaggeratePointCloudZ;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.MultibeamToPointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.surface.Delauny2D;
import pt.up.fe.dceg.neptus.plugins.vtk.surface.Delauny3D;
import pt.up.fe.dceg.neptus.plugins.vtk.surface.PointCloudMesh;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import vtk.vtkActor;
import vtk.vtkActorCollection;
import vtk.vtkCanvas;
import vtk.vtkCellArray;
import vtk.vtkLODActor;
import vtk.vtkLinearExtrusionFilter;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkTextActor;
import vtk.vtkVectorText;

/**
 * @author hfq
 * 
 */
public class MultibeamToolBar {

    private JToggleButton zExaggerationToggle;
    private JToggleButton rawPointsToggle;
    private JToggleButton downsampledPointsToggle;
    private JToggleButton meshToogle;
    private JToggleButton contoursToogle;
    
    private JButton resetViewportButton;
    private JButton helpButton;
    private JButton configButton;
    
    private JPanel toolBar;

    private vtkCanvas canvas;
    private LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud;

    private PointCloud<PointXYZ> pointCloud;
    private ExaggeratePointCloudZ exaggeZ;

    private vtkTextActor textProcessingActor;
    private vtkTextActor textZExagInfoActor;
    private Vtk vtk;

    private int currentPtsToIgnore;
    private boolean currentApproachToIgnorePts;
    private long currentTimestampMultibeamIncrement;
    private boolean currentYawMultibeamIncrement;
    private int currentZexagge;
    
    public MultibeamToolBar(Vtk vtk) {
        this.canvas = vtk.vtkCanvas;
        this.linkedHashMapCloud = vtk.linkedHashMapCloud;
        this.textProcessingActor = new vtkTextActor();
        this.textZExagInfoActor = new vtkTextActor();
        this.vtk = vtk;
        this.currentApproachToIgnorePts = vtk.approachToIgnorePts;
        this.currentPtsToIgnore = vtk.ptsToIgnore;
        this.currentTimestampMultibeamIncrement = vtk.timestampMultibeamIncrement;
        this.currentYawMultibeamIncrement = vtk.yawMultibeamIncrement;
        this.currentZexagge = vtk.zExaggeration;
        
        buildTextProcessingActor();
        buildTextZExagInfoActor();
        
        setToolBar(new JPanel());
    }

    /**
     * 
     */
    private void buildTextZExagInfoActor() {
        textZExagInfoActor.GetTextProperty().BoldOn();
        textZExagInfoActor.GetTextProperty().ItalicOn();
        textZExagInfoActor.GetTextProperty().SetColor(1.0, 1.0, 1.0);
        textZExagInfoActor.GetTextProperty().SetFontFamilyToArial();
        textZExagInfoActor.GetTextProperty().SetFontSize(12);
        textZExagInfoActor.SetInput(I18n.textf("Depth multipled by: %currentZexagge", currentZexagge));   //  
        textZExagInfoActor.VisibilityOn();
    }

    /**
     * 
     */
    private void buildTextProcessingActor() {
       textProcessingActor.GetTextProperty().BoldOn();
       textProcessingActor.GetTextProperty().ItalicOn();
       textProcessingActor.GetTextProperty().SetFontSize(40);
       textProcessingActor.GetTextProperty().SetColor(1.0, 0.0, 0.0);
       textProcessingActor.GetTextProperty().SetFontFamilyToArial();
       textProcessingActor.SetInput(I18n.text("Processing data..."));
       textProcessingActor.VisibilityOn();   
    }
    
    private String msgHelp() {
        
        String msgHelp;        
        //<h1>3D Multibeam Interaction</h1>
        msgHelp = "<html><font size='2'><br><div align='center'><table border='1' align='center'>" +
        		"<tr><th>Keys</th><th>Description</th></tr>" +
                "<tr><td>p, P</td><td>Switch to a point-based representation</td>" +
        		"<tr><td>w, W </td><td>Switch to a wireframe-based representation, when available</td>" +
                "<tr><td>s, S</td><td>Switch to a surface-based representation, when available</td>" +
                "<tr><td>j, J</td><td>Take a .PNG snapshot of the current window view</td>" +
                "<tr><td>g, G</td><td>Display scale grid (on/off)</td>" +
                "<tr><td>u, U</td><td>Display lookup table (on/off)</td>" +
                "<tr><td>r, R</td><td>Reset camera view along the current view direction</td>" +    // (to viewpoint = {0, 0, 0} -> center {x, y, z}\n");
                "<tr><td>i, I</td><td>Information about rendered cloud</td>" +
                "<tr><td>f, F</td><td>Fly Mode - point with mouse cursor the direction and press 'f' to fly</td>" +
                "<tr><td>+/-</td><td>Increment / Decrement overall point size</td>" +
                "<tr><td>3</td><td>Toggle into an out of stereo mode</td>" +
                "<tr><td>7</td><td>Color gradient in relation with X coords (north)</td>" +
                "<tr><td>8</td><td>Color gradient in relation with Y coords (west)</td>" +
                "<tr><td>9</td><td>Color gradient in relation with Z coords (depth)</td>" +
                "<tr><th>Mouse</th><th>Description</th></tr>" +
                // rotate the camera around its focal point. The rotation is in the direction defined from the center of the renderer's viewport towards the mouse position
                "<tr><td>Left mouse button</td><td>Rotate camera around its focal point</td>" +
                "<tr><td>Middle mouse button</td><td>Pan camera</td>" +
                "<tr><td>Right mouse button</td><td>Zoom (In/Out) the camera</td>" +
                "<tr><td>Mouse wheel</td><td>Zoom (In/Out) the camera - Static focal point</td>";
        
        return msgHelp;
    }

    /**
     * @return
     */
    public void createToolBar() {
        getToolBar().setLayout(new BoxLayout(getToolBar(), BoxLayout.X_AXIS));
        getToolBar().setBackground(Color.LIGHT_GRAY);

        // toolbar.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        // toolbar.setAutoscrolls(true);
        // Rectangle rect = new Rectangle();
        // rect.height = 50;
        // rect.height = 50;
        // toolbar.setBounds(rect);

        rawPointsToggle = new JToggleButton(I18n.text("Raw"));
        //rawPointsToggle.setBounds(getToolBar().getX(), getToolBar().getY(), getToolBar().getWidth(), 10);
        downsampledPointsToggle = new JToggleButton(I18n.text("Downsampled"));
        //downsampledPointsToggle.setBounds(rawPointsToggle.getBounds());

        zExaggerationToggle = new JToggleButton(I18n.text("Exaggerate Z"));

        meshToogle = new JToggleButton(I18n.text("Show Mesh"));
        contoursToogle = new JToggleButton(I18n.text("Show terrain contours"));

        resetViewportButton = new JButton(I18n.text("Reset Viewport"));
        helpButton = new JButton(I18n.text("Help"));

        rawPointsToggle.setSelected(true);
        downsampledPointsToggle.setSelected(false);
        meshToogle.setSelected(false);
        zExaggerationToggle.setSelected(false);
        contoursToogle.setSelected(false);
        //helpButton.setSize(10, 10);
        
        helpButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {              
                GuiUtils.htmlMessage(ConfigFetch.getSuperParentFrame() == null ? vtk : ConfigFetch.getSuperParentAsFrame()
                        , "Help for the 3D visualization interaction", "(3D Multibeam keyboard and mouse interaction)", msgHelp(), ModalityType.MODELESS);
            }
        });

        rawPointsToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rawPointsToggle.isSelected()) {
                    try {
                        if (meshToogle.isSelected())
                            meshToogle.setSelected(false);

                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = canvas.GetRenderer().GetActors();
                        actorCollection.InitTraversal();

                        canvas.lock();
                        
                        for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            vtkLODActor tempActor = new vtkLODActor();
                            tempActor = (vtkLODActor) actorCollection.GetNextActor();
                            Set<String> setOfMeshs = vtk.linkedHashMapMesh.keySet();
                            for (String sKey : setOfMeshs) {
                                PointCloudMesh mesh = vtk.linkedHashMapMesh.get(sKey);
                                if (tempActor.equals(mesh)) {
                                    canvas.GetRenderer().RemoveActor(tempActor);
                                }
                            }
                        }
                        Set<String> setOfClouds = linkedHashMapCloud.keySet();
                        for (String sKey : setOfClouds) {
                            canvas.GetRenderer().AddActor(linkedHashMapCloud.get(sKey).getCloudLODActor());
                        }
                        canvas.unlock();
                        
                        canvas.GetRenderer().ResetCamera();
                        canvas.Render();
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                else {
                    Set<String> setOfClouds = linkedHashMapCloud.keySet();
                    for (String sKey : setOfClouds) {
                        canvas.GetRenderer().RemoveActor(linkedHashMapCloud.get(sKey).getCloudLODActor());
                    }
                    canvas.Render();
                }
            }
        });

        downsampledPointsToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (downsampledPointsToggle.isSelected()) {
                    // try {
                    // System.out.println("Before collection");
                    // vtkActorCollection actorCollection = new vtkActorCollection();
                    // actorCollection = vtkCanvas.GetRenderer().GetActors();
                    // actorCollection.InitTraversal();
                    // for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                    // vtkCanvas.GetRenderer().RemoveActor(actorCollection.GetNextActor());
                    // }
                    // System.out.println("After collection");
                    //
                    // vtkCanvas.GetRenderer().Render();
                    //
                    // PointCloud<PointXYZ> downsampledCloud = new PointCloud<>();
                    //
                    // if (!isDownsampleDone) {
                    // PointCloud<PointXYZ> multibeamCloud = new PointCloud<>();
                    // multibeamCloud = linkedHashMapCloud.get("multibeam");
                    //
                    // performDownsample = new DownsamplePointCloud(multibeamCloud, 0.5);
                    //
                    // downsampledCloud = performDownsample.getOutputDownsampledCloud();
                    // linkedHashMapCloud.put(downsampledCloud.getCloudName(), downsampledCloud);
                    // }
                    // vtkCanvas.GetRenderer().AddActor(downsampledCloud.getCloudLODActor());
                    // }
                    // catch (Exception e1) {
                    // e1.printStackTrace();
                    // }
                }
                else {

                }
            }
        });

        meshToogle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (meshToogle.isSelected()) {        
                    try {
                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = canvas.GetRenderer().GetActors();
                        actorCollection.InitTraversal();

                        canvas.GetRenderer().AddActor(textProcessingActor);
                        textProcessingActor.SetDisplayPosition(canvas.getWidth() / 3, canvas.getHeight() / 2);
                        canvas.Render();
                        
                        //canvas.lock();
                        
                        for(int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            vtkLODActor tempActor = new vtkLODActor();
                            tempActor = (vtkLODActor) actorCollection.GetNextActor();
                            Set<String> setOfClouds;
                            setOfClouds = linkedHashMapCloud.keySet();
                            for (String sKey : setOfClouds) {
                                pointCloud = linkedHashMapCloud.get(sKey);
                                if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                    if (zExaggerationToggle.isSelected()) {
                                        exaggeZ.reverseZExaggeration();
                                        zExaggerationToggle.setSelected(false);
                                    }
                                    if (rawPointsToggle.isSelected()) {
                                        rawPointsToggle.setSelected(false);
                                    }
                                    canvas.GetRenderer().RemoveActor(pointCloud.getCloudLODActor());
                                    canvas.Render();
                                                                     
                                    //Delauny3D delauny = new Delauny3D(pointCloud);
                                    Delauny2D delauny = new Delauny2D(pointCloud);
                                    delauny.performDelauny();
                                    PointCloudMesh pointCloudMesh = new PointCloudMesh();
                                    pointCloudMesh.setMeshCloudLODActor(delauny.getDelaunyActor());
                                    canvas.GetRenderer().AddActor(pointCloudMesh.getMeshCloudLODActor());
                                    vtk.linkedHashMapMesh.put("multibeam", pointCloudMesh);
                                }
                            }
                        }
                        //canvas.unlock();
                        canvas.GetRenderer().ResetCamera();
                        canvas.GetRenderer().RemoveActor(textProcessingActor);
                        canvas.Render();
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                else {

                }
            }
        });

        zExaggerationToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (zExaggerationToggle.isSelected()) {
                    try {
//                        Rectangle rect = new Rectangle();
//                        rect = canvas.getBounds();                            
//                        NeptusLog.pub().info("Rectangle: height: " + rect.getHeight() + " width: " + rect.getWidth());                                 
//                        NeptusLog.pub().info("Baseline: height: " + canvas.getHeight() + " width: " + canvas.getWidth());

                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = canvas.GetRenderer().GetActors();
                        actorCollection.InitTraversal();
                        textZExagInfoActor.SetDisplayPosition(10, canvas.getHeight() - 20); 
                        canvas.GetRenderer().AddActor(textZExagInfoActor);
                        
                        for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            vtkLODActor tempActor = new vtkLODActor();
                            tempActor = (vtkLODActor) actorCollection.GetNextActor();
                            Set<String> setOfClouds;
                            setOfClouds = linkedHashMapCloud.keySet();
                            for (String sKey : setOfClouds) {
                                pointCloud = linkedHashMapCloud.get(sKey);
                                if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                    pointCloud.getCloudLODActor().VisibilityOff();
                                    textProcessingActor.GetTextProperty().SetJustificationToCentered();
                                    textProcessingActor.SetDisplayPosition(canvas.getWidth() / 3, canvas.getHeight() / 2);
                                    canvas.GetRenderer().AddActor(textProcessingActor);
                                    canvas.Render();
                                    exaggeZ = new ExaggeratePointCloudZ(pointCloud, vtk.zExaggeration);
                                    exaggeZ.performZExaggeration();
                                    canvas.GetRenderer().RemoveActor(textProcessingActor);
                                    canvas.Render();
                                    canvas.GetRenderer().ResetCamera();
                                    pointCloud.getCloudLODActor().VisibilityOn();
                                    canvas.GetRenderer().ResetCamera();
                                    canvas.Render();
                                }                                
                            }
                        }                        
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                else {
                    try {                
                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = canvas.GetRenderer().GetActors();
                        actorCollection.InitTraversal();
                        canvas.GetRenderer().RemoveActor(textZExagInfoActor);
                        
                        for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            vtkLODActor tempActor = new vtkLODActor();
                            tempActor = (vtkLODActor) actorCollection.GetNextActor();
                            Set<String> setOfClouds;
                            setOfClouds = linkedHashMapCloud.keySet();
                            for (String sKey : setOfClouds) {
                                pointCloud = linkedHashMapCloud.get(sKey);
                                if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                    pointCloud.getCloudLODActor().VisibilityOff();
                                    canvas.GetRenderer().AddActor(textProcessingActor);
                                    canvas.Render();
                                    exaggeZ.reverseZExaggeration();
                                    canvas.GetRenderer().RemoveActor(textProcessingActor);
                                    canvas.Render();
                                    canvas.GetRenderer().ResetCamera();
                                    pointCloud.getCloudLODActor().VisibilityOn();
                                    canvas.GetRenderer().ResetCamera();
                                    canvas.Render();
                                }
                            }
                        }
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        
        contoursToogle.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (contoursToogle.isSelected()) {
                    try {
                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = canvas.GetRenderer().GetActors();
                        actorCollection.InitTraversal();
                        
                        for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            vtkLODActor tempActor = new vtkLODActor();
                            tempActor = (vtkLODActor) actorCollection.GetNextActor();
                            Set<String> setOfClouds;
                            setOfClouds = linkedHashMapCloud.keySet();
                            for (String sKey : setOfClouds) {
                                pointCloud = linkedHashMapCloud.get(sKey);
                                if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                    Contours contours = new Contours(pointCloud);
                                    contours.generateTerrainContours2();
                                    canvas.GetRenderer().AddActor(contours.planeActor);
                                    canvas.Render();
                                    canvas.GetRenderer().ResetCamera();
                                }
                            }
                        }
                    }
                    catch (Exception e1) {
                        e1.getStackTrace();
                    }
                }
                else {
                    
                }
            }
        });
        

        resetViewportButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.GetRenderer().ResetCamera();
                canvas.getRenderWindowInteractor().Render();
            }
        });

        configButton = new JButton(new AbstractAction(I18n.text("Configure")) {    
            private static final long serialVersionUID = -1404112253602290953L;

            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(vtk,
                        SwingUtilities.getWindowAncestor(vtk), true);

                if (vtk.ptsToIgnore != currentPtsToIgnore || vtk.approachToIgnorePts != currentApproachToIgnorePts || vtk.timestampMultibeamIncrement != currentTimestampMultibeamIncrement || vtk.yawMultibeamIncrement != currentYawMultibeamIncrement) {
                    pointCloud = linkedHashMapCloud.get("multibeam");

                    canvas.lock();
                    canvas.GetRenderer().RemoveActor(pointCloud.getCloudLODActor());
                    if (zExaggerationToggle.isSelected()) {
                        exaggeZ.reverseZExaggeration();
                        canvas.GetRenderer().RemoveActor(textProcessingActor);
                        zExaggerationToggle.setSelected(false);
                    }
                    canvas.unlock();
                            
                    textProcessingActor.SetDisplayPosition(canvas.getWidth() / 3, canvas.getHeight() / 2);
                    canvas.GetRenderer().AddActor(textProcessingActor);
                    canvas.Render();
                    
                    canvas.lock();                 
                        // clean up cloud color handlers
                    pointCloud.setPoints(new vtkPoints());
                    pointCloud.setVerts(new vtkCellArray());
                    pointCloud.setPoly(new vtkPolyData());
                    pointCloud.setCloudLODActor(new vtkLODActor());
                    pointCloud.setColorHandler(new PointCloudHandlers<>());
                    
                    vtk.multibeamToPointCloud = new MultibeamToPointCloud(vtk.getLog(), pointCloud);
                    vtk.multibeamToPointCloud.parseMultibeamPointCloud(vtk.approachToIgnorePts, vtk.ptsToIgnore, vtk.timestampMultibeamIncrement, vtk.yawMultibeamIncrement);
                    
                    if (pointCloud.getNumberOfPoints() != 0) {
                        pointCloud.createLODActorFromPoints();               
                        canvas.unlock();
                        
                        canvas.GetRenderer().RemoveActor(textProcessingActor);
                        
                        canvas.GetRenderer().AddActor(pointCloud.getCloudLODActor());
                        vtk.winCanvas.getInteractorStyle().getScalarBar().getScalarBarActor().Modified();
                        
                        canvas.GetRenderer().ResetCamera();
                        canvas.Render();   
                    }
                    else {
                        canvas.unlock();
                        
                        canvas.GetRenderer().RemoveActor(textProcessingActor);
                        
                        String msgErrorMultibeam;
                        msgErrorMultibeam = I18n.text("No beams on Log file!");
                        JOptionPane.showMessageDialog(null, msgErrorMultibeam);
                        
                        vtkVectorText vectText = new vtkVectorText();
                        vectText.SetText(I18n.text("No beams on Log file!"));
                        
                        vtkLinearExtrusionFilter extrude = new vtkLinearExtrusionFilter();
                        extrude.SetInputConnection(vectText.GetOutputPort());
                        extrude.SetExtrusionTypeToNormalExtrusion();
                        extrude.SetVector(0, 0, 1);
                        extrude.SetScaleFactor(0.5);
                   
                        vtkPolyDataMapper txtMapper = new vtkPolyDataMapper();
                        txtMapper.SetInputConnection(extrude.GetOutputPort());
                        vtkActor txtActor = new vtkActor();
                        txtActor.SetMapper(txtMapper);
                        txtActor.SetPosition(2.0, 2.0, 2.0);
                        txtActor.SetScale(10.0);
                        
                        
                        vtk.vtkCanvas.GetRenderer().AddActor(txtActor);    
                    }
                    currentPtsToIgnore = vtk.ptsToIgnore;
                    currentApproachToIgnorePts = vtk.approachToIgnorePts;
                    currentTimestampMultibeamIncrement = vtk.timestampMultibeamIncrement;
                    currentYawMultibeamIncrement = vtk.yawMultibeamIncrement;
                }
            }
        });
        
            // toogles
        getToolBar().add(rawPointsToggle);
        //getToolBar().add(downsampledPointsToggle);
        getToolBar().add(meshToogle);
        getToolBar().add(zExaggerationToggle);
        //getToolBar().add(contoursToogle);
        
        getToolBar().add(new JSeparator(JSeparator.VERTICAL), BorderLayout.LINE_START);
        
            // buttons
        getToolBar().add(resetViewportButton);
        getToolBar().add(configButton);
        getToolBar().add(helpButton);
    }

    /**
     * @return the toolBar
     */
    public JPanel getToolBar() {
        return toolBar;
    }

    /**
     * @param toolBar the toolBar to set
     */
    private void setToolBar(JPanel toolBar) {
        this.toolBar = toolBar;
    }
}
