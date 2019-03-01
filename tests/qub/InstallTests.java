package qub;

public class InstallTests
{
    public static void test(TestRunner runner)
    {
        runner.testGroup(Install.class, () ->
        {
            runner.testGroup("main(String[])", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubTest.main((String[])null), new PreConditionFailure("args cannot be null."));
                });
            });

            runner.testGroup("main(Console)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> main((Console)null), new PreConditionFailure("console cannot be null."));
                });

                runner.test("with /? command line argument", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    try (final Console console = createConsole(output, "/?"))
                    {
                        main(console);
                        test.assertEqual(-1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Usage: qub-install [[-folder=]<folder-path-to-install>] [-verbose]",
                            "  Installs source code projects into the Qub folder.",
                            "  -folder: The folder to install from. This can be specified either with the",
                            "           -folder argument name or without it.",
                            "  -verbose: Whether or not to show verbose logs."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with -? command line argument", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    try (final Console console = createConsole(output, "-?"))
                    {
                        main(console);
                        test.assertEqual(-1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Usage: qub-install [[-folder=]<folder-path-to-install>] [-verbose]",
                            "  Installs source code projects into the Qub folder.",
                            "  -folder: The folder to install from. This can be specified either with the",
                            "           -folder argument name or without it.",
                            "  -verbose: Whether or not to show verbose logs."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with showTotalDuration set to false", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "/i/dont/exist"))
                    {
                        main(console, false);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "ERROR: The file at \"/i/dont/exist/project.json\" doesn't exist."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with showTotalDuration set to true", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "/i/dont/exist"))
                    {
                        main(console, true);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "ERROR: The file at \"/i/dont/exist/project.json\" doesn't exist."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with unnamed folder argument to rooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "/i/dont/exist"))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "ERROR: The file at \"/i/dont/exist/project.json\" doesn't exist."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with named folder argument to unrooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "i/dont/exist"))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "ERROR: The file at \"/i/dont/exist/project.json\" doesn't exist."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with named folder argument to rooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "-folder=/i/dont/exist"))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "ERROR: The file at \"/i/dont/exist/project.json\" doesn't exist."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with named folder argument to unrooted folder that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder, "-folder=i/dont/exist"))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "ERROR: The file at \"/i/dont/exist/project.json\" doesn't exist."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with no project.json file", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    try (final Console console = createConsole(output, currentFolder))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "ERROR: The file at \"/project.json\" doesn't exist."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with no java property in the project.json file", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    currentFolder.getFile("project.json").await()
                        .setContentsAsString(JSON.object(projectJson ->
                        {
                        }).toString())
                        .await();
                    try (final Console console = createConsole(output, currentFolder))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "ERROR: No language specified in project.json. Nothing to compile."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with no source code files", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    currentFolder.getFile("project.json").await()
                        .setContentsAsString(JSON.object(projectJson ->
                        {
                            projectJson.objectProperty("java");
                        }).toString())
                        .await();
                    try (final Console console = createConsole(output, currentFolder))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "ERROR: No java source files found in /."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with no project property", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    currentFolder.getFile("project.json").await()
                        .setContentsAsString(JSON.object(projectJson ->
                        {
                            projectJson.objectProperty("java");
                        }).toString())
                        .await();
                    currentFolder.getFile("sources/A.java").await()
                        .setContentsAsString("A.java source").await();
                    try (final Console console = createConsole(output, currentFolder))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Creating jar file...",
                            "Running tests...",
                            "",
                            "Installing...",
                            "ERROR: A project property must be specified in the project.json file."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with no jar file", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    currentFolder.getFile("project.json").await()
                        .setContentsAsString(JSON.object(projectJson ->
                        {
                            projectJson.stringProperty("project", "fake-project");
                            projectJson.objectProperty("java");
                        }).toString())
                        .await();
                    currentFolder.getFile("sources/A.java").await()
                        .setContentsAsString("A.java source").await();
                    try (final Console console = createConsole(output, currentFolder, "-createjar=false"))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Running tests...",
                            "",
                            "Installing...",
                            "ERROR: Couldn't find a compiled project jar file at /outputs/fake-project.jar."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with no publisher property", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    currentFolder.getFile("project.json").await()
                        .setContentsAsString(JSON.object(projectJson ->
                        {
                            projectJson.stringProperty("project", "fake-project");
                            projectJson.objectProperty("java");
                        }).toString())
                        .await();
                    currentFolder.getFile("sources/A.java").await()
                        .setContentsAsString("A.java source").await();
                    try (final Console console = createConsole(output, currentFolder))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Creating jar file...",
                            "Running tests...",
                            "",
                            "Installing...",
                            "ERROR: A publisher property must be specified in the project.json file."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with no version property", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    currentFolder.getFile("project.json").await()
                        .setContentsAsString(JSON.object(projectJson ->
                        {
                            projectJson.stringProperty("project", "fake-project");
                            projectJson.stringProperty("publisher", "fake-publisher");
                            projectJson.objectProperty("java");
                        }).toString())
                        .await();
                    currentFolder.getFile("sources/A.java").await()
                        .setContentsAsString("A.java source").await();
                    try (final Console console = createConsole(output, currentFolder))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Creating jar file...",
                            "Running tests...",
                            "",
                            "Installing...",
                            "ERROR: A version property must be specified in the project.json file."),
                        Strings.getLines(output.getText().await()).skipLast());
                });

                runner.test("with version property", (Test test) ->
                {
                    final InMemoryCharacterStream output = getInMemoryCharacterStream(test);
                    final Folder currentFolder = getInMemoryCurrentFolder(test);
                    currentFolder.getFile("project.json").await()
                        .setContentsAsString(JSON.object(projectJson ->
                        {
                            projectJson.stringProperty("project", "fake-project");
                            projectJson.stringProperty("publisher", "fake-publisher");
                            projectJson.stringProperty("version", "fake-version");
                            projectJson.objectProperty("java");
                        }).toString())
                        .await();
                    currentFolder.getFile("sources/A.java").await()
                        .setContentsAsString("A.java source").await();
                    try (final Console console = createConsole(output, currentFolder))
                    {
                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Creating jar file...",
                            "Running tests...",
                            "",
                            "Installing...",
                            "ERROR: A version property must be specified in the project.json file."),
                        Strings.getLines(output.getText().await()).skipLast());
                });
            });
        });
    }

    private static InMemoryCharacterStream getInMemoryCharacterStream(Test test)
    {
        return new InMemoryCharacterStream(test.getParallelAsyncRunner());
    }

    private static InMemoryFileSystem getInMemoryFileSystem(Test test)
    {
        PreCondition.assertNotNull(test, "test");

        final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getParallelAsyncRunner(), test.getClock());
        fileSystem.createRoot("/");

        return fileSystem;
    }

    private static Folder getInMemoryCurrentFolder(Test test)
    {
        PreCondition.assertNotNull(test, "test");

        return getInMemoryFileSystem(test).getFolder("/").await();
    }

    private static Console createConsole(CharacterWriteStream output, String... commandLineArguments)
    {
        PreCondition.assertNotNull(output, "output");
        PreCondition.assertNotNull(commandLineArguments, "commandLineArguments");

        final Console result = new Console(Iterable.create(commandLineArguments));
        result.setLineSeparator("\n");
        result.setOutput(output);

        return result;
    }

    private static Console createConsole(CharacterWriteStream output, Folder currentFolder, String... commandLineArguments)
    {
        PreCondition.assertNotNull(output, "output");
        PreCondition.assertNotNull(currentFolder, "currentFolder");
        PreCondition.assertNotNull(commandLineArguments, "commandLineArguments");

        final Console result = createConsole(output, commandLineArguments);
        result.setFileSystem(currentFolder.getFileSystem());
        result.setCurrentFolderPath(currentFolder.getPath());

        PostCondition.assertNotNull(result, "result");

        return result;
    }

    private static void main(Console console)
    {
        main(console, null);
    }

    private static void main(Console console, Boolean showTotalDuration)
    {
        PreCondition.assertNotNull(console, "console");

        final Build build = new Build();
        build.setJavaCompiler(new FakeJavaCompiler());
        build.setJarCreator(new FakeJarCreator());

        final QubTest qubTest = new QubTest();
        qubTest.setJavaRunner(new FakeJavaRunner());
        qubTest.setBuild(build);

        final Install install = new Install();
        if (showTotalDuration != null)
        {
            install.setShowTotalDuration(showTotalDuration);
        }
        install.setQubTest(qubTest);

        install.main(console);
    }
}
