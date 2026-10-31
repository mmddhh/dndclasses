package mdh.dndclasses.client;

import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.screen.ApricityScreen;
import mdh.dndclasses.network.DndNetwork;
import mdh.dndclasses.network.SubclassChoicePacket;
import net.minecraft.client.Minecraft;

/** AUI page listing the subclasses the player may pick. */
public class SubclassScreen extends ApricityScreen {

    private static final String PAGE = "screens/subclass.html";
    private boolean injected;

    public SubclassScreen() {
        super(PAGE);
    }

    @Override
    public void tick() {
        super.tick();
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        Document document = getLinkedDocument();
        if (document == null) {
            return;
        }
        if (!injected) {
            Element data = document.getElementById("choice-data");
            if (data != null) {
                data.setValue(ChoiceScreenOpener.json());
                injected = true;
            }
        }
        Element payload = document.getElementById("choice-payload");
        if (payload == null) {
            return;
        }
        String value = payload.getValue();
        if (value == null || value.isEmpty()) {
            return;
        }
        payload.setValue("");
        DndNetwork.CHANNEL.sendToServer(new SubclassChoicePacket(value));
    }
}
