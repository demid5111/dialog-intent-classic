package intent.classic;

import org.apache.commons.cli.*;

class DICLIUtils {
    static Options getCLIOptions() {
        Options options = new Options();

        Option input = new Option("i", "input-dir", true, "path to input dir with graphs");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output-dir", true, "path to output dir");
        output.setRequired(true);
        options.addOption(output);

        Option useSuperTypes = new Option("general", "generalize-types", true, "flag to use or not generalization of intents");
        useSuperTypes.setRequired(true);
        options.addOption(useSuperTypes);

        Option algos = new Option("algos", "algorithms", true, "names of algorithms to use");
        algos.setRequired(true);
        options.addOption(algos);

        Option runs = new Option("n", "number-runs", true, "number of runs");
        runs.setRequired(true);
        options.addOption(runs);

        return options;
    }

    static CommandLine parseCLIArgs(String[] args, Options options) {
        CommandLine cmd = null;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }
        return cmd;
    }
}
