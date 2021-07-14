package com.ryanthetechman.death_detector.client;

import com.fazecast.jSerialComm.SerialPort;
import com.ryanthetechman.death_detector.util.ClientChatCallback;
import com.ryanthetechman.death_detector.util.EntityOnDeathCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class Death_detectorClient implements ClientModInitializer {
    private static final int shockWait = 5000;

    private static boolean wasDead = false;
    private static float lastHealth = 0;
    private static String lastTeamMsg = "";
    private static ClientWorld lastClientWorld = null;

    private static int hypixelStep = 0;
    private static boolean hypixelIsDead = false;

    private static SerialPort comPort;
    private static OutputStream comOut;
    private static InputStream comIn;
    public static boolean debugMode = false;
    private static boolean serialAttached = true;
    private static int currentCOMPort = 4;
    private static int shockTime = 500;

    private static KeyBinding debugKeyBinding;
    private static KeyBinding detachSerial;
    private static KeyBinding comUP;
    private static KeyBinding comDOWN;
    private static KeyBinding timeUP;
    private static KeyBinding timeDOWN;


    private static long lastTimeShocked = System.currentTimeMillis();

    public static final Logger LOGGER = (Logger) LogManager.getLogger("Death Detector");

    @Override
    public void onInitializeClient() {
        debugKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.death_detector.debug_mode",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "category.death_detector.main"
        ));

        detachSerial = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.death_detector.detach_serial",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "category.death_detector.main"
        ));

        comUP = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.death_detector.increase_com",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                "category.death_detector.main"
        ));

        comDOWN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.death_detector.decrease_com",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET,
                "category.death_detector.main"
        ));

        timeUP = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.death_detector.increase_time",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                "category.death_detector.main"
        ));

        timeDOWN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.death_detector.decrease_time",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_MINUS,
                "category.death_detector.main"
        ));

        comPort = SerialPort.getCommPort("COM" + currentCOMPort);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        comPort.setBaudRate(9600);

        comPort.openPort();
        comOut = comPort.getOutputStream();
        comIn = comPort.getInputStream();
        updateShockTime();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            /*try
            {
                if (comIn.available() > 0) {
                    for (byte b : comIn.readAllBytes()) {
                        LOGGER.warn("Got Data: " + b);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }*/

            while (comUP.wasPressed()){
                if (serialAttached){
                    client.inGameHud.getChatHud().addMessage(Text.of("\n>>> Please Disconnect from Serial Port First! (Press '" + KeyBindingHelper.getBoundKeyOf(detachSerial).getTranslationKey().substring(13).toUpperCase() + "')\n"));
                    break;
                }
                currentCOMPort++;
                client.inGameHud.getChatHud().addMessage(Text.of("\n>>> COM Port Increased to COM" + currentCOMPort + ".\nPress '" + KeyBindingHelper.getBoundKeyOf(detachSerial).getTranslationKey().substring(13).toUpperCase() + "' to enable port." + "\n"));
            }

            while (comDOWN.wasPressed()){
                if (serialAttached){
                    client.inGameHud.getChatHud().addMessage(Text.of("\n>>> Please Disconnect from Serial Port First! (Press '" + KeyBindingHelper.getBoundKeyOf(detachSerial).getTranslationKey().substring(13).toUpperCase() + "')\n"));
                    break;
                }
                currentCOMPort = Math.max((currentCOMPort - 1), 1);
                client.inGameHud.getChatHud().addMessage(Text.of("\n>>> COM Port Decreased to COM" + currentCOMPort + ".\nPress '" + KeyBindingHelper.getBoundKeyOf(detachSerial).getTranslationKey().substring(13).toUpperCase() + "' to enable port." + "\n"));
            }

            while (timeUP.wasPressed()){
                if (shockTime <= 1) shockTime = 0;
                shockTime += 100;
                client.inGameHud.getChatHud().addMessage(Text.of("\n>>> Shock Time Increased to " + String.format("%.2f", (shockTime)/1000f) + "s.\n"));
            }

            while (timeDOWN.wasPressed()){
                shockTime = Math.max((shockTime - 100), 1);
                client.inGameHud.getChatHud().addMessage(Text.of("\n>>> Shock Time Decreased to " + String.format("%.2f", (shockTime)/1000f) + "s.\n"));
            }

            while (detachSerial.wasPressed()){
                if (serialAttached) {
                    try {comOut.write(45);} catch (IOException ignore) {}
                    try {comOut.close();} catch (IOException ignore) {}
                    try {comIn.close();} catch (IOException ignore) {}
                    comPort.closePort();
                    client.inGameHud.getChatHud().addMessage(Text.of("\n>>> Disconnected from Serial\n"));
                    serialAttached = false;
                }else{
                    comPort = SerialPort.getCommPort("COM" + currentCOMPort);
                    comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
                    comPort.openPort();
                    comOut = comPort.getOutputStream();
                    comIn = comPort.getInputStream();

                    client.inGameHud.getChatHud().addMessage(Text.of("\n>>> Connected to Serial\n"));
                    serialAttached = true;

                    updateShockTime();
                }
            }

            ClientPlayerEntity player = client.player;
            if (player == null) return;

            if (player.isDead()) {
                if (!wasDead) {
                    wasDead = true;
                    //Player is now dead
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("\n[Death Detector] §cPlayer Died. Shock Triggered\n"));
                    LOGGER.warn("Player Died. Shock Triggered");
                    triggerShock();
                }
            } else if (wasDead) {
                wasDead = false;
                //Player is now alive
                /*try {
                    comOut.write45);
                } catch (IOException e) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("\n[Death Detector] §cError! Could not connect to Serial!\n"));
                }*/
            }

            while (debugKeyBinding.wasPressed()) {
                if (debugMode) {
                    debugMode = false;
                    client.inGameHud.getChatHud().addMessage(Text.of("\n>>> Debug Mode: Disabled\n"));
                } else {
                    debugMode = true;
                    client.inGameHud.getChatHud().addMessage(Text.of("\n>>> Debug Mode: Enabled\n"));
                }
            }

            AbstractTeam team = client.player.getScoreboardTeam();
            ClientWorld cw = client.player.clientWorld;
            if (cw != lastClientWorld){
                if (debugMode) client.inGameHud.getChatHud().addMessage(Text.of(">>> World Changed"));
                lastClientWorld = cw;
                hypixelStep = 0;
                hypixelIsDead = false;
            }

            if (team == null) {
                if (!lastTeamMsg.equals("none")) {
                    if (debugMode) client.inGameHud.getChatHud().addMessage(Text.of(">>> Player Not In Team"));
                    LOGGER.warn("Player Not In Team");lastTeamMsg = "none";
                }
            } else {
                if (!lastTeamMsg.equals(team.getName() + team.getColor())) {
                    if (debugMode) client.inGameHud.getChatHud().addMessage(Text.of(">>> Team Name: " + team.getName()));
                    LOGGER.warn(">>> Team Name: " + team.getName());
                    if (debugMode) client.inGameHud.getChatHud().addMessage(Text.of(">>> Color: " + team.getColor().name()));
                    LOGGER.warn(">>> Color: " + team.getColor().name());
                    lastTeamMsg = team.getName() + team.getColor();
                }
            }


            if (team != null){
                if (!hypixelIsDead && hypixelStep == 0 && team.getName().replaceAll("§[0-9A-FK-ORa-fkor]", "").endsWith("pre")){ //Player joined game, is in setup
                    hypixelStep = 1;
                    if (debugMode) client.inGameHud.getChatHud().addMessage(Text.of("\n>>> Joined Pre Match\n"));
                    LOGGER.warn("Joined Pre Match");
                }
                else if (hypixelStep == 2 && team.getColor().name().equals("GREEN")){ //Player goes from being assigned to joining team.
                    hypixelStep = 3;
                    if (debugMode) client.inGameHud.getChatHud().addMessage(Text.of("\n>>> Joined Team\n"));
                    LOGGER.warn("Joined Team");
                }
                else if (hypixelStep == 3 && team.getColor().name().equals("GRAY")){ //Player has died
                    hypixelStep = 0;
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("\n[Death Detector] §cHypixel Team Change. Shock Triggered\n"));
                    LOGGER.warn("Hypixel Team Change. Shock Triggered");
                    hypixelIsDead = true;
                    triggerShock();
                }
            }
            else if(hypixelStep == 1){//Player is being assigned a team
                hypixelStep = 2;
                if (debugMode) client.inGameHud.getChatHud().addMessage(Text.of("\n>>> Assigning Team\n"));
                LOGGER.warn("Assigning Team");
            }

            /*float health = player.getHealth();
            if (health == 0) {
                player.sendMessage(Text.of("I Love Poop"), false);
                LOGGER.warn("PLAYER HEALTH IS 0");
            }
            if (health != lastHealth) {
                LOGGER.info(health);
                lastHealth = health;
            }*/

            /*final List<ChatHudLine<Text>> messages = ((ChatAccessor) client.inGameHud.getChatHud()).getMessages();
            String msg = (messages.size() > 0 ? messages.get(0).getText().getString() : "");
            if (!lastMsg.equals(msg)){
                lastMsg = msg;
                LOGGER.info("MSG: " + msg);
            }*/
        });

        ClientChatCallback.EVENT.register(message -> {
            final String msg = message.getString();
            //LOGGER.info("MSG: " + msg);
            if (!hypixelIsDead) {
                if (msg.startsWith("You died!")){
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("\n[Death Detector] §cYou Died. Shock Triggered\n"));
                    LOGGER.warn("You Died. Shock Triggered");
                    triggerShock();
                    hypixelIsDead = true;
                }
                else {
                    final ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    final boolean valid = player != null;
                    if (valid && msg.startsWith(player.getName().getString() + " fell") && !player.isDead()){
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("\n[Death Detector] §cFell into Void. Shock Triggered\n"));
                        LOGGER.warn("Fell into Void. Shock Triggered");
                        triggerShock();
                        hypixelIsDead = true;
                    }
                }
            }
        });

        /*EntityOnDeathCallback.EVENT.register(source -> {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("\n[Death Detector] §3Sksksk player died xD imagine being that bad.\n"));
            System.out.println("HEhehe");
        });*/
    }

    private void updateShockTime() {
        try {comOut.write(40);} catch (IOException ignore) {} //Queue for shockTime
        try {comOut.write(shockTime/10);} catch (IOException ignore) {}
    }

    private void triggerShock() {
        final long time = System.currentTimeMillis();
        if (lastTimeShocked < (time - shockWait)) {
            if (comPort.isOpen()) {
                lastTimeShocked = System.currentTimeMillis();
                try {
                    comOut.write(47);
                } catch (IOException e) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("\n[Death Detector] §cError! Could not connect to Serial!\n"));
                }
                if (debugMode) MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("\n>>> Shock Triggered\n"));
                LOGGER.warn("Shock Triggered!!!");
            } else if (debugMode) MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("\n>>> Serial Not Connected!\n"));
        }
        else if (debugMode) MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("\n>>> Shock NOT Triggered. Too close to last shock! (" + String.format("%.2f", (shockWait - (time - lastTimeShocked))/1000f) + "s left)\n"));
    }
}