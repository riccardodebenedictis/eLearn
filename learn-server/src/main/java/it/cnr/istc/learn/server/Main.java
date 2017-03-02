/*
 * Copyright (C) 2017 Riccardo De Benedictis <riccardo.debenedictis@istc.cnr.it>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.cnr.istc.learn.server;

import it.cnr.istc.learn.server.resources.Users;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.UriBuilder;
import org.apache.activemq.broker.BrokerService;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author Riccardo De Benedictis <riccardo.debenedictis@istc.cnr.it>
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(UriBuilder.fromUri("http://localhost/8080").build(), new ResourceConfig(
                Users.class
        ));
        try {
            server.start();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        BrokerService broker = new BrokerService();

        try {
            // configure the broker
            broker.addConnector("mqtt://localhost:1883");
            broker.start();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        try {
            MqttClient mqtt_client = new MqttClient("tcp://localhost:1883", "learn-server");
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            mqtt_client.connect(options);

            mqtt_client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    LOG.log(Level.SEVERE, null, cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    LOG.log(Level.INFO, "message arrived on topic {0}: {1}", new Object[]{topic, message.toString()});
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    LOG.info("message delivered");
                }
            });
        } catch (MqttException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
}
