package net.minecraft;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.src.AbstractClientPlayer;
import net.minecraft.src.Minecraft;

public class ThreadFetchSkinType extends Thread{
    private String url;
    private final AbstractClientPlayer player;

    public ThreadFetchSkinType(AbstractClientPlayer player){
        this.player = player;
        this.url = new String("https://api.mojang.com/users/profiles/minecraft/" + player.username);
    }
    
    //Gonna be honest, I was mostly copying ThreadDownloadImageDataINNER1 and then improvising with gson
    //Happy this mess of code works somehow
    public void run(){
        HttpURLConnection site = null;
        HttpURLConnection site2 = null;
        String url2;

        //Let's see if this backfires badly
        boolean isDone = false;

        while(!isDone){
            try {
                site = (HttpURLConnection) (new URL(this.url)).openConnection(Minecraft.getMinecraft().getProxy());
                site.setDoInput(true);
                site.setDoOutput(false);
                site.connect();

                if (site.getResponseCode() / 100 == 2) {
                    // This bit is here to get the UUID
                    BufferedReader reader = new BufferedReader(new InputStreamReader(site.getInputStream()));
                    JsonObject dataPartone = new JsonParser().parse(reader).getAsJsonObject();
                    url2 = new String(
                            "https://api.mojang.com/session/minecraft/profile/" + dataPartone.get("id").getAsString());

                    // System.out.println(url + "\n" + dataPartone.get("name").getAsString());
                    site2 = (HttpURLConnection) (new URL(url2)).openConnection(Minecraft.getMinecraft().getProxy());
                    site2.setDoInput(true);
                    site2.setDoOutput(false);
                    site2.connect();

                    if (site2.getResponseCode() / 100 == 2) {
                        // Now this is the fun one
                        // Mojang stores the textures in a properties array in base64
                        // Then there's even more objects until the bit I actually want, model

                        // I could probably rewrite this to handle capes (or get the url at least)
                        // ...or just rewrite ThreadDownloadImageInner1
                        BufferedReader reader2 = new BufferedReader(new InputStreamReader(site2.getInputStream()));

                        JsonArray data = new JsonParser().parse(reader2).getAsJsonObject().getAsJsonArray("properties");
                        String base64String = data.get(0).getAsJsonObject().get("value").getAsString();

                        JsonObject moreShit = new JsonParser()
                                .parse(new String(Base64.getDecoder().decode(base64String))).getAsJsonObject();

                        // apparantly skins can just, not have the metadata jsonobject so check for that
                        if(moreShit.getAsJsonObject("textures").getAsJsonObject("SKIN").has("metadata")){
                            String skinType = moreShit.getAsJsonObject("textures").getAsJsonObject("SKIN")
                                .getAsJsonObject("metadata").get("model").getAsString();

                            System.out.println("Obtained skin type! " + skinType);
                            player.setSkinType(skinType);
                        }else{
                            System.out.println("User does not have metadata object! Using default");
                        }

                        // Fuck it, while we're here, gonna store UUID
                        player.setUUID(dataPartone.get("id").getAsString());
                        isDone = true;
                    }
                }

                //Is there a better way of doing this? Probably
                if(!isDone){
                    TimeUnit.SECONDS.sleep(3);
                }

            } catch (Exception err) {
                err.printStackTrace();
                return;
            } finally {
                if (site != null) {
                    site.disconnect();
                }
                if (site2 != null) {
                    site2.disconnect();
                }
            }
        }
        
    }
}
