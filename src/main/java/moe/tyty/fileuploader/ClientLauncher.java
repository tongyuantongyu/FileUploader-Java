package moe.tyty.fileuploader;

import com.ea.async.Async;
import org.apache.commons.cli.*;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

public class ClientLauncher {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
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

        String _host = cmd.getOptionValue("h");
        String _port = cmd.getOptionValue("p");
        int port;
        String key = cmd.getOptionValue("k");
        String file = cmd.getOptionValue("f");

        if (_host == null || _port == null || key == null || file == null) {
            System.out.println("One or more required parameters not given.");
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("Options", options);
            System.exit(1);
            return;
        }

        try {
            port = Integer.parseUnsignedInt(_port);
            if (!(port > 0 && port < 65536)) {
                System.out.println("port parameter should be an int in range (0, 65536).");
                System.exit(1);
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("port parameter should be an number.");
            System.exit(1);
            return;
        }

        InetAddress host = null;

        try {
            if (cmd.hasOption("4") && cmd.hasOption("6")) {
                System.out.println("-4 and -6 should not be used together.");
                System.exit(1);
                return;
            } else if (!cmd.hasOption("4") && !cmd.hasOption("6")) {
                host = InetAddress.getByName(_host);
            } else if (cmd.hasOption("4")) {
                for (InetAddress address : InetAddress.getAllByName(_host)) {
                    if (address instanceof Inet4Address) {
                        host = address;
                        break;
                    }
                }
            } else {
                for (InetAddress address : InetAddress.getAllByName(_host)) {
                    if (address instanceof Inet6Address) {
                        host = address;
                        break;
                    }
                }
            }
            if (host == null) {
                throw new UnknownHostException();
            }
        } catch (UnknownHostException e) {
            System.out.println("No available ip to the given host.");
            System.exit(1);
            return;
        }

        System.out.printf("Server: [%s]:%d\n", host.getHostAddress(), port);

        if (key.length() < 8) {
            System.out.println("You are using a short key which is weak to be attacked.");
        }

        File _path = Paths.get(file).toFile();
        if (!_path.isFile() || !_path.canRead()) {
            System.out.println("The file can't be opened for reading.");
            System.exit(1);
            return;
        }
        System.out.printf("File: %s\n", file);

        String _thread = cmd.getOptionValue("t");
        int thread = 1;
        if (_thread != null) {
            try {
                thread = Integer.parseUnsignedInt(_thread);
                if (thread < 1) {
                    System.out.println("thread parameter should be an unsigned int.");
                    System.exit(1);
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("thread parameter should be an number.");
                System.exit(1);
                return;
            }
        }

        String _size = cmd.getOptionValue("s");
        int size = 65536;
        if (_size != null) {
            try {
                size = Integer.parseUnsignedInt(_size);
                if (size < 1) {
                    System.out.println("size parameter should be an unsigned int.");
                    System.exit(1);
                    return;
                }
                if (size < 64) {
                    System.out.println("Your size parameter is too small." +
                            " We'll go on, but transfer can be inefficient.");
                }
            } catch (NumberFormatException e) {
                System.out.println("size parameter should be an number.");
                System.exit(1);
                return;
            }
        }

        System.out.printf("Using thread: %d, piece size: %d\n", thread, size);

        Client client = new Client(host, port, key, file, thread, size);
        Async.init();
        if (client.runSession().get()) {
            System.exit(0);
        }
        else {
            System.exit(1);
        }
    }
}
