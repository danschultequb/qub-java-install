package qub;

public class Install
{
    private QubTest qubTest;
    private Boolean showTotalDuration;

    public void setQubTest(QubTest qubTest)
    {
        this.qubTest = qubTest;
    }

    public QubTest getQubTest()
    {
        if (qubTest == null)
        {
            qubTest = new QubTest();
        }
        final QubTest result = qubTest;

        PostCondition.assertNotNull(result, "result");

        return result;
    }

    public void setShowTotalDuration(boolean showTotalDuration)
    {
        this.showTotalDuration = showTotalDuration;
    }

    public boolean getShowTotalDuration()
    {
        if (showTotalDuration == null)
        {
            showTotalDuration = true;
        }
        return showTotalDuration;
    }

    public void main(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        if (shouldShowUsage(console))
        {
            console.writeLine("Usage: qub-install [[-folder=]<folder-path-to-install>] [-verbose]");
            console.writeLine("  Installs source code projects into the Qub folder.");
            console.writeLine("  -folder: The folder to install from. This can be specified either with the");
            console.writeLine("           -folder argument name or without it.");
            console.writeLine("  -verbose: Whether or not to show verbose logs.");
            console.setExitCode(-1);
        }
        else
        {
            final boolean showTotalDuration = getShowTotalDuration();
            final Stopwatch stopwatch = console.getStopwatch();
            if (showTotalDuration)
            {
                stopwatch.start();
            }
            try
            {
                final QubTest qubTest = getQubTest();
                qubTest.setShowTotalDuration(false);
                qubTest.main(console);

                if (console.getExitCode() == 0)
                {
                    console.writeLine("Installing...");

                    final Folder folderToInstall = getFolderToInstall(console);
                    final File projectJsonFile = folderToInstall.getFile("project.json").await();
                    final ProjectJSON projectJson = ProjectJSON.parse(projectJsonFile).await();
                    final String project = projectJson.getProject();
                    if (Strings.isNullOrEmpty(project))
                    {
                        error(console, "A project property must be specified in the project.json file.");
                    }
                    else
                    {
                        final Folder outputFolder = folderToInstall.getFolder("outputs").await();
                        final File projectJarFile = outputFolder.getFile(projectJson.getProject() + ".jar").await();

                        if (!projectJarFile.exists().await())
                        {
                            error(console, "Couldn't find a compiled project jar file at " + projectJarFile.toString() + ".");
                        }
                        else
                        {
                            final String qubHome = console.getEnvironmentVariable("QUB_HOME");
                            final Folder qubFolder = console.getFileSystem().getFolder(qubHome).await();

                            final String publisher = projectJson.getPublisher();
                            if (Strings.isNullOrEmpty(publisher))
                            {
                                error(console, "A publisher property must be specified in the project.json file.");
                            }
                            else
                            {
                                final Folder publisherFolder = qubFolder.getFolder(publisher).await();

                                final String version = projectJson.getVersion();
                                if (Strings.isNullOrEmpty(version))
                                {
                                    error(console, "A version property must be specified in the project.json file.");
                                }
                                else
                                {
                                    final Folder versionFolder = publisherFolder.getFolder(version).await();
                                    if (versionFolder.exists().await())
                                    {
                                        error(console, "This package (" + publisher + "/" + project + ":" + version + ") can't be installed because a package with that signature already exists.");
                                    }
                                    else
                                    {
                                        final File installedProjectJsonFile = versionFolder.getFile("project.json").await();
                                        verbose(console, "Copying " + projectJsonFile.toString() + " to " + installedProjectJsonFile.toString() + "...").await();
                                        projectJsonFile.copyTo(installedProjectJsonFile).await();

                                        final File installedProjectJarFile = versionFolder.getFile(projectJarFile.getName()).await();
                                        verbose(console, "Copying " + projectJarFile.toString() + " to " + installedProjectJarFile.toString() + "...");
                                        projectJarFile.copyTo(installedProjectJarFile).await();

                                        final ProjectJSONJava projectJsonJava = projectJson.getJava();
                                        if (projectJsonJava != null)
                                        {
                                            final String mainClass = projectJsonJava.getMainClass();
                                            if (!Strings.isNullOrEmpty(mainClass))
                                            {
                                                String shortcutName = projectJsonJava.getShortcutName();
                                                if (Strings.isNullOrEmpty(shortcutName))
                                                {
                                                    shortcutName = installedProjectJarFile.getNameWithoutFileExtension();
                                                }

                                                String classpath = "%~dp0" + installedProjectJarFile.relativeTo(qubFolder);
                                                for (final Dependency dependency : projectJsonJava.getDependencies())
                                                {
                                                    final File dependencyFile = qubFolder
                                                        .getFolder(dependency.getPublisher()).await()
                                                        .getFolder(dependency.getProject()).await()
                                                        .getFolder(dependency.getVersion()).await()
                                                        .getFile(dependency.getProject()).await();
                                                    classpath += ";%~dp0" + dependencyFile.toString();
                                                }

                                                final File shortcutFile = qubFolder.getFile(shortcutName + ".cmd").await();
                                                final String shortcutFileContents =
                                                    "@echo OFF\n" +
                                                    "java -cp " + classpath + " " + mainClass + " %*\n";
                                                verbose(console, "Writing " + shortcutFile.toString() + "...");
                                                shortcutFile.setContentsAsString(shortcutFileContents).await();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            finally
            {
                if (showTotalDuration)
                {
                    final Duration compilationDuration = stopwatch.stop().toSeconds();
                    console.writeLine("Done (" + compilationDuration.toString("0.0") + ")").await();
                }
            }
        }
    }

    private static boolean shouldShowUsage(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        return console.getCommandLine().contains(
            (CommandLineArgument argument) ->
            {
                final String argumentString = argument.toString();
                return argumentString.equals("/?") || argumentString.equals("-?");
            });
    }

    private static Path getFolderPathToInstall(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        Path result = null;
        final CommandLine commandLine = console.getCommandLine();
        if (commandLine.any())
        {
            CommandLineArgument folderArgument = commandLine.get("folder");
            if (folderArgument == null)
            {
                folderArgument = commandLine.getArguments()
                    .first((CommandLineArgument argument) -> argument.getName() == null);
            }
            if (folderArgument != null)
            {
                result = Path.parse(folderArgument.getValue());
            }
        }

        if (result == null)
        {
            result = console.getCurrentFolderPath();
        }

        if (!result.isRooted())
        {
            result = console.getCurrentFolderPath().resolve(result).await();
        }

        PostCondition.assertNotNull(result, "result");
        PostCondition.assertTrue(result.isRooted(), "result.isRooted()");

        return result;
    }

    private static Folder getFolderToInstall(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        final Folder result = console.getFileSystem().getFolder(getFolderPathToInstall(console)).await();

        PostCondition.assertNotNull(result, "result");

        return result;
    }

    public static boolean isVerbose(Console console)
    {
        boolean result = false;

        CommandLineArgument verboseArgument = console.getCommandLine().get("verbose");
        if (verboseArgument != null)
        {
            final String verboseArgumentValue = verboseArgument.getValue();
            result = Strings.isNullOrEmpty(verboseArgumentValue) ||
                Booleans.isTrue(java.lang.Boolean.valueOf(verboseArgumentValue));
        }

        return result;
    }

    public static Result<Void> verbose(Console console, String message)
    {
        return verbose(console, false, message);
    }

    public static Result<Void> verbose(Console console, boolean showTimestamp, String message)
    {
        PreCondition.assertNotNull(console, "console");
        PreCondition.assertNotNull(message, "message");

        Result<Void> result = Result.success();
        if (isVerbose(console))
        {
            result = console.writeLine("VERBOSE" + (showTimestamp ? "(" + System.currentTimeMillis() + ")" : "") + ": " + message)
                .then(() -> {});
        }

        PostCondition.assertNotNull(result, "result");

        return result;
    }

    public static Result<Void> error(Console console, String message)
    {
        return error(console, false, message);
    }

    public static Result<Void> error(Console console, boolean showTimestamp, String message)
    {
        PreCondition.assertNotNull(console, "console");
        PreCondition.assertNotNull(message, "message");

        final Result<Void> result = console.writeLine("ERROR" + (showTimestamp ? "(" + System.currentTimeMillis() + ")" : "") + ": " + message).then(() -> {});
        console.incrementExitCode();

        PostCondition.assertNotNull(result, "result");

        return result;
    }

    public static void main(String[] args)
    {
        Console.run(args, (Console console) -> new Install().main(console));
    }
}