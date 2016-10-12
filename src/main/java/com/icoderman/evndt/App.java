package com.icoderman.evndt;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.icoderman.evndt.util.DeployUtil;
import com.icoderman.evndt.util.ZipUtil;

public class App {

    public static void main(String[] args) {
        System.out.println("EVN Deploy Tool");
        AppOptions options = new AppOptions();
        try {
            new JCommander(options, args);
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }

        String compressedProject = compressProject(options.getWorkspace(), options.getProject());
        System.out.println("Project " + options.getProject() + " has been successfully compressed and saved at: " + compressedProject);
        if (DeployUtil.deployProject(options, compressedProject)) {
            System.out.println("Project " + options.getProject() +" has been successfully deployed to " + options.getServer());
        }
    }

    private static String compressProject(String workspace, String projectName) {
        String projectDirectory = workspace + projectName;
        String compressedProject =  projectDirectory + ".zip";
        ZipUtil.createZip(projectDirectory, compressedProject);
        return compressedProject;
    }

}
