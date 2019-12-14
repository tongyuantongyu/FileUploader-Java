package moe.tyty.fileuploader;

import com.ea.async.Async;
import moe.tyty.fileuploader.Exception.BadOptionException;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * CLI launcher to the client
 */
public class ClientLauncher {
    @SuppressWarnings("DuplicatedCode")
    public static void main(String[] args) {
        Options options = new Options();

        options.addOption("h", "host",
                true, "Host of the server upload to");
        options.addOption("p", "port",
                true, "Port of the server upload to");
        options.addOption("k", "key",
                true, "Key used to encrypt data");
        options.addOption("f", "file",
                true, "Path to the file to be upload");

        options.addOption("t", "thread",
                true, "thread count to be used to upload");
        options.addOption("s", "size",
                true, "size of the file piece (in byte)");
        options.addOption("r", "remote",
                true, "path for remote to save in");

        options.addOption("?","help", false, "show help");

        options.addOption("4", false, "Use IPv4 only");
        options.addOption("6", false, "Use IPv6 only");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Options given are unrecognizable.");
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("Options", options);
            System.exit(1);
            return;
        }

        if (cmd.hasOption("?")) {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("Options", options);
            return;
        }

        Client.OptionPack option = Client.buildOption(
                cmd.getOptionValue("h"),
                cmd.getOptionValue("p"),
                cmd.getOptionValue("k"),
                cmd.getOptionValue("f"),
                cmd.getOptionValue("t"),
                cmd.getOptionValue("s"),
                cmd.getOptionValue("r"),
                cmd.hasOption("4"),
                cmd.hasOption("6")
                );

        Client client = new Client(option);
        Async.init();
        try {
            double timeConsume = client.runSession().get();
            if (timeConsume != (double) -1) {
                System.out.printf("File transfer finished in %f seconds.\n", timeConsume);
            }
        } catch (BadOptionException e) {
            System.out.println("Bad option:" + e.getMessage());
            if (e.showHelp) {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp("Options", options);
            }
            System.exit(1);
        }
        catch (RuntimeException | InterruptedException | ExecutionException | IOException e) {
            System.err.printf("File transfer failed: %s\nStackTrace: \n", e.getMessage());
            e.printStackTrace();
        }
    }
}
