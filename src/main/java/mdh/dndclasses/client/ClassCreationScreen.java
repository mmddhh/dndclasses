package mdh.dndclasses.client;

import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.screen.ApricityScreen;
import mdh.dndclasses.network.ClassSelectionPacket;
import mdh.dndclasses.network.DndNetwork;
import net.minecraft.client.Minecraft;

public class ClassCreationScreen extends ApricityScreen {

    private static final String PAGE = "screens/class_creation.html";
    private static final String PAYLOAD_ELEMENT_ID = "class-payload";
    private static final String STATE_ELEMENT_ID = "page-state";

    private String lastMode = "";

    public ClassCreationScreen() {
        super(PAGE);
    }

    @Override
    public void tick() {
        super.tick();

        Document document = getLinkedDocument();
        if (document == null) {
            return;
        }

        if (!"create".equals(lastMode)) {
            Element stateElement = document.getElementById(STATE_ELEMENT_ID);
            if (stateElement != null) {
                lastMode = "create";
                stateElement.setValue("{\"mode\":\"create\"}");
            }
        }

        Element payloadElement = document.getElementById(PAYLOAD_ELEMENT_ID);
        if (payloadElement == null) {
            return;
        }

        String payload = payloadElement.getValue();
        if (payload == null || payload.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return;
        }

        payloadElement.setValue("");
        DndNetwork.CHANNEL.sendToServer(new ClassSelectionPacket(payload));
    }
}
