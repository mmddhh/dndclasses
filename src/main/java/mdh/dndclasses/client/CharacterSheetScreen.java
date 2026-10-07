package mdh.dndclasses.client;

import com.google.gson.JsonObject;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.screen.ApricityScreen;

/**
 * Read-only character sheet. Reuses the class-creation page but switches it into
 * "sheet mode" by feeding the synced character state through a hidden input.
 */
public class CharacterSheetScreen extends ApricityScreen {

    private static final String PAGE = "screens/class_creation.html";
    private static final String PAYLOAD_ELEMENT_ID = "page-state";

    private String lastPayload = "";

    public CharacterSheetScreen() {
        super(PAGE);
    }

    @Override
    public void tick() {
        super.tick();

        Document document = getLinkedDocument();
        if (document == null) {
            return;
        }

        Element element = document.getElementById(PAYLOAD_ELEMENT_ID);
        if (element == null) {
            return;
        }

        String json = buildJson();
        if (!json.equals(lastPayload)) {
            lastPayload = json;
            element.setValue(json);
        }
    }

    private static String buildJson() {
        JsonObject root = new JsonObject();
        root.addProperty("mode", "sheet");
        root.addProperty("class", ClientCharacterState.classKey);
        root.addProperty("subclass", ClientCharacterState.subclassKey);
        root.addProperty("level", ClientCharacterState.level);
        root.addProperty("exp", ClientCharacterState.exp);
        root.addProperty("expLevelStart", ClientCharacterState.expLevelStart);
        root.addProperty("expToNext", ClientCharacterState.expToNext);
        root.addProperty("proficiency", ClientCharacterState.proficiency);
        root.addProperty("created", ClientCharacterState.created);

        JsonObject abilities = new JsonObject();
        int[] values = ClientCharacterState.abilities;
        abilities.addProperty("str", at(values, 0));
        abilities.addProperty("dex", at(values, 1));
        abilities.addProperty("con", at(values, 2));
        abilities.addProperty("int", at(values, 3));
        abilities.addProperty("wis", at(values, 4));
        abilities.addProperty("cha", at(values, 5));
        root.add("abilities", abilities);

        return root.toString();
    }

    private static int at(int[] values, int index) {
        return values != null && index < values.length ? values[index] : 8;
    }
}
