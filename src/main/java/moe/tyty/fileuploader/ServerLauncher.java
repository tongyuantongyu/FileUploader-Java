package moe.tyty.fileuploader;

import com.ea.async.Async;
import org.apache.commons.cli.*;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * CLI Server Launcher
 */
public class ServerLauncher {
    public static void main(String[] args) {
        Options options = new Options();

        options.addOption("h", "host",
                true, "The ip address the server should bind to");
        options.addOption("p", "port",
                true, "Port of the server bind to");
        options.addOption("k", "key",
                true, "Key used to encrypt data");

        options.addOption("?", "help", false, "show help");

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

        if (_port == null || key == null) {
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
                if (_host == null) {
                    host = InetAddress.getByName("::");
                } else {
                    host = InetAddress.getByName(_host);
                }
            } else if (cmd.hasOption("4")) {
                if (_host == null) {
                    host = InetAddress.getByName("0.0.0.0");
                } else {
                    for (InetAddress address : InetAddress.getAllByName(_host)) {
                        if (address instanceof Inet4Address) {
                            host = address;
                            break;
                        }
                    }
                }
            } else {
                if (_host == null) {
                    host = InetAddress.getByName("::");
                } else {
                    for (InetAddress address : InetAddress.getAllByName(_host)) {
                        if (address instanceof Inet6Address) {
                            host = address;
                            break;
                        }
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

        if (key.length() < 8) {
            System.out.println("You are using a short key which is weak to be attacked.");
        }

        Server server = new Server(host, port, key);
        Async.init();
        server.bootstrap();
    }
}
