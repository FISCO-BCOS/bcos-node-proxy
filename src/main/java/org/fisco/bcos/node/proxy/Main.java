package org.fisco.bcos.node.proxy;

public class Main {

    public static void main(String[] args) {
        System.out.println("Try to start proxy server, waiting ... ");
        final String clientConfigName = "client/config.toml";
        final String serverConfigName = "server/config.toml";
        final String clientConfigPath =
                Main.class.getClassLoader().getResource(clientConfigName).getPath();
        final String serverConfigFile =
                Main.class.getClassLoader().getResource(serverConfigName).getPath();
        Server server = new Server(clientConfigPath, serverConfigFile);
        if (server.start()) {
            System.out.println("Start proxy server successfully");
        } else {
            System.out.println("Start proxy server failed");
            return;
        }
    }
}
