# PoC: Artemis 2.21.0 (MQTT 5) as a deployment package for Kura 5.6.x

Throw-away proof of concept. Kura 5.6.x embeds ActiveMQ Artemis 2.6.4 in its core distribution, which only
speaks MQTT 3.1/3.1.1. Artemis 2.21.0 is the first release with an MQTT 5 broker. This module builds a
deployment package (`.dp`) that replaces the Artemis bundles of an existing Kura 5.6.x installation, without
rebuilding Kura.

## What is in the DP

| Bundle | Version | Notes |
|---|---|---|
| `org.apache.activemq.artemis` | 2.21.0 | Single wrapper bundle, same layout as the Kura 5.6 one. Embeds the Artemis jars, `activemq-artemis-native` 1.0.2, `jboss-logging` 3.4.2 (Kura ships 3.3.2, Artemis needs `[3.4,4)`), `jctools-core`, `commons-configuration2`, `commons-text`, `micrometer-core`, `netty-codec-socks` and `netty-handler-proxy`. Everything else (Netty 4.1.135, Guava 32.1.1, commons-beanutils/collections/lang3/logging, JMS 2.0 and JTA specs) is imported from the Kura runtime. |
| `org.apache.activemq.artemis-mqtt-protocol` | 2.21.0 | Upstream bundle re-wrapped only to relax the Guava import range (`[30.1,31)` upstream, Kura ships 32.1.1). |
| `org.eclipse.kura.broker.artemis.core` | 1.8.0 | Kura 5.6 sources, adapted to the 2.21.0 `FileDeploymentManager.buildService` signature. |
| `org.eclipse.kura.broker.artemis.simple.mqtt` | 1.7.0 | Kura 5.6 sources, unchanged. Same component PID and metatype, so existing configurations keep working. |
| `org.eclipse.kura.broker.artemis.xml` | 1.7.0 | Kura 5.6 sources, unchanged. |

No configuration change is needed for MQTT 5: the Artemis MQTT acceptor negotiates 3.1, 3.1.1 or 5 based on
the client CONNECT packet.

## Requirements

- **Java 11+ at runtime.** Artemis 2.21.0 is compiled for Java 11. Kura 5.6 runs on Java 17 by default; a
  device running Kura on Java 8 cannot use this DP (the bundles stay unresolved).
- Java 11+ JDK to build (tested with JDK 17).

## Build

This is a plain Maven build, independent from the Kura Tycho build:

```bash
cd poc/artemis-2.21.0-dp
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn clean install
```

The DP is generated in `distrib/target/org.eclipse.kura.broker.artemis.poc_<version>.dp`.

## Install on a Kura 5.6.x device

Kura 5.6 runs Equinox with `osgi.clean=true`: at every start the framework state is wiped and the bundles
listed in `osgi.bundles` of `config.ini` are installed again. Uninstalling the old Artemis bundles from the
OSGi console is therefore not persistent; they must be removed from `config.ini`.

1. Stop Kura: `systemctl stop kura`
2. Back up `config.ini`, in the `framework` folder of the Kura installation
   (typically `/opt/eclipse/kura/framework/config.ini`).
3. Remove from the `osgi.bundles` property the entries of the six bundles below, including the leading `, `
   separator of each entry:
   - `org.apache.activemq.artemis_2.12.0.jar`
   - `org.apache.activemq.artemis-mqtt-protocol_2.6.4.jar`
   - `org.apache.activemq.artemis-native_2.6.4.jar`
   - `org.eclipse.kura.broker.artemis.core_1.7.*.jar`
   - `org.eclipse.kura.broker.artemis.simple.mqtt_1.6.*.jar`
   - `org.eclipse.kura.broker.artemis.xml_1.6.*.jar`

   For example:

   ```bash
   cp config.ini config.ini.bak
   sed -i -E 's#, ?reference:file:[^,]*/(org\.apache\.activemq\.artemis(-mqtt-protocol|-native)?|org\.eclipse\.kura\.broker\.artemis\.(core|simple\.mqtt|xml))_[^,]*\.jar@[^,]*##g' config.ini
   grep -o 'artemis[^,]*' config.ini   # must print nothing
   ```

   The other bundles that were added for Artemis (Netty, Guava, commons-beanutils/collections, Geronimo
   specs, jboss-logging 3.3.2) stay: the DP uses them, or ignores them.
4. Start Kura: `systemctl start kura`
5. Install the DP from the Kura web UI (*Packages* → *Install/Upgrade*) or with the `DEPLOY-V2` cloud
   command. Kura reinstalls it at every start, as for any other DP.
6. Enable the broker in *Services* → *Simple Artemis MQTT Broker* (or keep the existing configuration).

Installing the DP **without** removing the old bundles first is not supported: both broker generations would
register components with the same PIDs.

To roll back: uninstall the DP, stop Kura, restore `config.ini.bak`, start Kura.

## Install on an ESF 7.6.x device

ESF differs from Kura 5.6 in three points:

- Artemis is not part of the base distribution: it is installed by the `com.eurotech.framework.amq7` extra DP,
  that also ships the JMS 2.0 and JTA 1.1 specs. `config.ini` does not need any change.
- The JMS and JTA specs must therefore be part of this DP. Build it with the `esf` profile:

  ```bash
  JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn clean install -Pesf
  ```

- The bundles of this DP are not signed. With the production security policy they get no permissions and stay
  unresolved, reporting an `Unresolved requirement: Import-Package: ...` error for the first package they import.
  The device must run in development mode (`permit-debug` set to `true` in `user/security/security.policy`).

Steps:

1. Uninstall the `com.eurotech.framework.amq7` DP, if installed, from *Packages*.
2. Install the DP built with `-Pesf` from *Packages* → *Install/Upgrade*.
3. Enable the broker in *Services* → *Simple Artemis MQTT Broker*.

## How it was verified

Off-device, in Equinox 3.16.0 on Java 17 with the same third party bundle versions shipped by Kura 5.6
(Netty 4.1.135, Guava 32.1.1, jboss-logging 3.3.2, Felix SCR 2.1.16, ...) and without the old Artemis bundles:

- all the DP bundles resolve and start;
- the `simple.mqtt` component, activated with `enabled=true`, starts Artemis 2.21.0 with the MQTT acceptor;
- an MQTT 5 client (Eclipse Paho mqttv5 1.2.5) publishes and receives a message with user properties and
  content type preserved;
- an MQTT 3.1.1 client publishes to the MQTT 5 subscriber.

Not verified yet: installation through the Kura `DeploymentAdmin` on a real device, the XML broker component,
TLS acceptors, persistence and the native AIO journal (`artemis-native` falls back to NIO, which is fine with
the default `persistence-enabled=false` of the simple broker).

## Known limitations / open points

- Artemis 2.21.0 dates from 2022 and was chosen as the minimum version with MQTT 5: check the security
  advisories fixed in later releases before using it beyond the PoC. `kura-artemis` for Kura 6 is on
  2.40.0, built for Java 21.
- Only the Artemis bundles are replaced; Kura cloud connections (Paho MQTT 3.1.1 client) are unaffected.
