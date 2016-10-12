package com.icoderman.evndt;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppOptions {
    @Parameter(names = {"--server"}, description = "Server and Port (host:port)", required = true)
    private String server;

    @Parameter(names = {"--user"}, description = "User name", required = true)
    private String user;

    @Parameter(names = {"--password"}, description = "Password", required = true)
    private String password;

    @Parameter(names= {"--project"}, description = "Project name", required = true)
    private String project;

    @Parameter(names= {"--workspace"}, description = "Workspace directory", required = true)
    private String workspace;
}
