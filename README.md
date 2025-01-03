Netnotes.io
----
Netnotes-Linux
===

This release is designed to be easily built and run on linux with minimal user setup requirements and currently is not designed to be run with a setup/laucher binary. 

*Currently taskbar support and the ability to run as a daemon has been removed from this release 

(This release utilises the bash shell, rather than cmd or powershell, and so windows users should utilize the windows release).

Installation Instructions
===
Latest linux .jar: https://github.com/networkspore/Netnotes-Linux/releases

(Built using openJDK 17.0.9)

*See https://github.com/networkspore/Netnotes/releases for Windows releases


This project is targeted toward Java 17+ jdk and and Maven.

Your default-jdk must be installed and set
~~~
sudo apt install default-jdk
~~~

The jar can be executed with:
~~~
java -jar <filename>.jar
(eg: java -jar netnotes-0.2.0.jar)
~~~

For Apple users:
---
The jar may require being built with the correct platform selected: 
~~~
mvn package -Djavafx.platform=mac-aarch64
~~~
(other mac options for platform are: 
~~~
mac (x86_64) mac-monocle (x86_64 monocle), and mac-aarch64-monocle
~~~
Uses the bash shell to launch and shutdown the node, and check the ping. By all accounts this should work in OSX but if it doesn't, let me know if you know a fix.

Description
===
An evolving desktop application which allows you to install and control your access to the Ergo Block chain and receive live crypto currency market data utilizing various API (KuCoin Exchange/ Spectrum Finanace) .

Future development aims to include the ability to communicate with the web browsers, utilizing a unique communication technique of passing notes.
Features

Networks
==

Ergo Network
-
Wallets: Based on the Ergo Appkit and the Satergo Wallet, you may create, add or restore wallets which are cross compatible with the Satergo .erg wallets. Utilizing Ergo Wallets you may send ergs and tokens, as well as view your transactions, and watch sent and custom transactions so that you may know when they are completed. Ergo Wallets integrates with the Ergo Explorers, Ergo Nodes and Ergo Markets in the Ergo Network in order to allow for modular customization.

Explorer: Currently set to use the default Ergo Platform explorer API, the ergo Explorers app (features in development) will allow you to setup access to explorers. *Explorers are what are used to view wallet information such as transaction and ballance data.

Ergo Tokens (refactoring): Add / remove or modify the way tokens display in your wallet. Ergo Tokens lets you ensure that your wallet can represent your newest tokens with the icon of your choosing, as well as has a list of default settings. Ergo tokens is the jump off point for token integration.

Ergo Market (refactoring)s: Allows you to receive, and utilize live price information (currently only supporting KuCoin) in your wallet.

Apps
===
Spectrum Finance (nearly refactored)
-
Connects to the spectrum API to get ergo price information, for base currency and tokens. The live chart allows for zooming in on the candlestick charts, which are displayed with a linear scale. In order crop the chart to focus in on the areas which you want to view more closely, you may use the cropping bar tool on the left hand side. Simply click the bar tool then click and drag the top end of the bar to the top of the chart which you wish to see, and then click and drag the bottom of the bar to the lowest data which you would like to see. You may then click the green button at the top of the bar, or the red button to reset the crop to normal.

Credits
===
Wallet / Node
--
1. Satergo's encoding/encryption and interfaece, original source found at: https://github.com/Satergo/Satergo
2. Ergo AppKid: A Library for Java/Scala/Kotlin Development of Ergo Applications: https://github.com/ergoplatform/ergo-appkit

Encryption
--
1. DevSkiller friendly id is used in place of creation of UUID, to allow for more favorable encoding:  https://github.com/Devskiller/friendly-id
2. Joubin Mohammad Houshyar <alphazero@sensesay.net> Java implentation of BLAKE2B cryptographic digest algorithm:  https://github.com/alphazero/Blake2b

Decoding
--
1. JavaFxSVG: A simple library to add SVG support to JavaFX, allowing to use SVG graphics just like any other image type. https://github.com/codecentric/javafxsvg