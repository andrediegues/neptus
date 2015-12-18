/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 17/12/2015
 */
package pt.lsts.neptus.mra;

import java.awt.Component;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;

import com.google.common.collect.Lists;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.bathymetry.TidePredictionFinder;

/**
 * @author pdias
 *
 */
public class TidesMraLoader {

    /** To avoid instantiation */
    private TidesMraLoader() {
    }

    public static  void load(IMraLogGroup source, Component parent) {
        String tideInfoPath = TidePredictionFactory.MRA_TIDE_INDICATION_FILE_PATH;
        String noTideStr = "<" + I18n.text("No tides") + ">";
        String otherTideStr = "<" + I18n.text("Other") + ">";
        String usedTideStr = noTideStr;
        
        File tideInfoFx = new File(source.getDir(), TidePredictionFactory.MRA_TIDE_INDICATION_FILE_PATH);
        if (tideInfoFx.exists() && tideInfoFx.canRead()) {
            String hF = FileUtil.getFileAsString(tideInfoFx);
            if (hF != null && !hF.isEmpty()) {
                File fx = new File(TidePredictionFactory.BASE_TIDE_FOLDER_PATH, hF);
                if (fx != null && fx.exists() && fx.canRead())
                    usedTideStr = hF;
            }
        }

        String ret = usedTideStr;
        
        // Choosing tide sources options
        String[] lstStringArray = TidePredictionFactory.getTidesFileAsStringList();
        Arrays.sort(lstStringArray);
        List<String> lst = Lists.asList(noTideStr, otherTideStr, lstStringArray);
        ret = (String) JOptionPane.showInputDialog(parent, I18n.text("Choose a tides source"), 
                I18n.text("Tides"), JOptionPane.QUESTION_MESSAGE, null, 
                lst.toArray(), usedTideStr);
        if (ret == null)
            return;

        Date startDate = new Date((long) (source.getLsfIndex().getStartTime() * 1E3));
        Date endDate = new Date((long) (source.getLsfIndex().getEndTime() * 1E3));
        
        // If other let us open we options
        if (otherTideStr.equals(ret)) {
            String harbor = TidePredictionFactory.fetchData(parent, null, startDate, endDate, true);
            if (harbor == null || harbor.isEmpty())
                return;
            else
                ret = harbor + "." + TidePredictionFactory.defaultTideFormat;
        }

        if (noTideStr.equals(ret)) {
            FileUtil.saveToFile(new File(source.getDir(), tideInfoPath).getAbsolutePath(), "");
        }
        else {
            String tName = ret;
            String msg = I18n.text("Trying to load tide data");
            // Needed for the TidePredictionFactory.create(..)
            FileUtil.saveToFile(new File(source.getDir(), tideInfoPath).getAbsolutePath(), tName);
            TidePredictionFinder tFinder = TidePredictionFactory.create(source);
            if (tFinder == null) {
                FileUtil.saveToFile(new File(source.getDir(), tideInfoPath).getAbsolutePath(), "");
                msg = I18n.text("Not possible to load tide file");
            }

            if (tFinder == null || !tFinder.contains(startDate) || !tFinder.contains(endDate)) {
                msg = I18n.text("Some tide data missing. Want to update tide predictions?");
                if (tFinder == null)
                    msg = I18n.text("No tide data found. Want to update tide predictions?");

                int updatePredictionsQuestion = GuiUtils.confirmDialog(parent, I18n.text("Tides"), msg);
                switch (updatePredictionsQuestion) {
                    case JOptionPane.YES_OPTION:
                        String harborFetch = TidePredictionFactory.fetchData(parent,
                                tFinder == null ? null : tFinder.getName(), startDate, endDate, true);
                        if (harborFetch != null)
                            FileUtil.saveToFile(new File(source.getDir(), tideInfoPath).getAbsolutePath(),
                                    harborFetch + "." + TidePredictionFactory.defaultTideFormat);
                        break;
                    default:
                        FileUtil.saveToFile(new File(source.getDir(), tideInfoPath).getAbsolutePath(), "");
                        break;
                }
            }
        }
    }
}
