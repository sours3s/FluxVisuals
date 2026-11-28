package ru.fluxvisuals.render.nametags;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.Identifier;
import ru.fluxvisuals.api.render.util.ResourceProvider;

/** Small SVG path loader used by the ported NameTags icons. */
final class NameTagSvg {
    private static final Pattern TOKEN = Pattern.compile(
          "([MmLlHhVvCcQqZz])|(-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?)"
    );
    private static final int MAX_SEGMENTS = 256;

    final float width;
    final float height;
    final float strokeWidth;
    final boolean filled;
    final float[] segments;

    private NameTagSvg(float width, float height, float strokeWidth, boolean filled, List<float[]> lines) {
        this.width = width;
        this.height = height;
        this.strokeWidth = strokeWidth;
        this.filled = filled;
        float[] packed = new float[Math.min(lines.size(), MAX_SEGMENTS) * 4];
        for (int i = 0; i < packed.length / 4; i++) {
            float[] line = lines.get(i);
            packed[i * 4] = line[0];
            packed[i * 4 + 1] = line[1];
            packed[i * 4 + 2] = line[2];
            packed[i * 4 + 3] = line[3];
        }
        this.segments = packed;
    }

    static NameTagSvg load(String key) {
        Identifier id = Identifier.of("fluxvisuals", "nametags/icons/" + key + ".json");
        JsonObject json = JsonParser.parseString(ResourceProvider.toString(id)).getAsJsonObject();
        float width = json.has("width") ? json.get("width").getAsFloat() : 16.0F;
        float height = json.has("height") ? json.get("height").getAsFloat() : 16.0F;
        float strokeWidth = json.has("strokeWidth") ? json.get("strokeWidth").getAsFloat() : 1.0F;
        boolean filled = json.has("filled") && json.get("filled").getAsBoolean();

        List<float[]> lines = new ArrayList<>();
        JsonArray paths = json.getAsJsonArray("paths");
        for (JsonElement element : paths) {
            appendFlattened(lines, element.getAsString());
        }
        return new NameTagSvg(width, height, strokeWidth, filled, lines);
    }

    int segmentCount() {
        return this.segments.length / 4;
    }

    private static void appendFlattened(List<float[]> lines, String pathData) {
        Path2D.Float path = new Path2D.Float();
        Matcher matcher = TOKEN.matcher(pathData);
        String command = null;
        float x = 0.0F;
        float y = 0.0F;
        float startX = 0.0F;
        float startY = 0.0F;
        float controlX = 0.0F;
        float controlY = 0.0F;
        List<Float> numbers = new ArrayList<>(8);

        while (matcher.find()) {
            String token = matcher.group();
            if (isCommand(token)) {
                if (command != null && !numbers.isEmpty()) {
                    Object[] state = applyCommand(path, command, numbers, x, y, startX, startY);
                    x = (Float) state[0];
                    y = (Float) state[1];
                    startX = (Float) state[2];
                    startY = (Float) state[3];
                    controlX = (Float) state[4];
                    controlY = (Float) state[5];
                    numbers.clear();
                }
                command = token;
            } else if (command != null) {
                numbers.add(Float.parseFloat(token));
            }
        }
        if (command != null && !numbers.isEmpty()) {
            Object[] state = applyCommand(path, command, numbers, x, y, startX, startY);
            x = (Float) state[0];
            y = (Float) state[1];
            startX = (Float) state[2];
            startY = (Float) state[3];
            controlX = (Float) state[4];
            controlY = (Float) state[5];
        }

        float[] coords = new float[6];
        float prevX = 0.0F;
        float prevY = 0.0F;
        PathIterator iterator = path.getPathIterator(null, 0.12);
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO) {
                prevX = coords[0];
                prevY = coords[1];
            } else if (type == PathIterator.SEG_LINETO) {
                lines.add(new float[]{prevX, prevY, coords[0], coords[1]});
                prevX = coords[0];
                prevY = coords[1];
            } else if (type == PathIterator.SEG_CLOSE) {
                lines.add(new float[]{prevX, prevY, startX, startY});
                prevX = startX;
                prevY = startY;
            }
            iterator.next();
        }
    }

    private static boolean isCommand(String token) {
        return token.length() == 1 && "MmLlHhVvCcQqZz".indexOf(token.charAt(0)) >= 0;
    }

    /** Returns x, y, startX, startY, controlX, controlY. */
    private static Object[] applyCommand(Path2D.Float path, String command, List<Float> n,
                                         float x, float y, float startX, float startY) {
        char op = command.charAt(0);
        boolean relative = Character.isLowerCase(op);
        int index = 0;
        float controlX = x;
        float controlY = y;

        switch (Character.toUpperCase(op)) {
            case 'M':
                while (index + 1 < n.size()) {
                    x = value(n.get(index++), relative, x);
                    y = value(n.get(index++), relative, y);
                    path.moveTo(x, y);
                    startX = x;
                    startY = y;
                }
                break;
            case 'L':
                while (index + 1 < n.size()) {
                    x = value(n.get(index++), relative, x);
                    y = value(n.get(index++), relative, y);
                    path.lineTo(x, y);
                }
                break;
            case 'H':
                while (index < n.size()) {
                    x = value(n.get(index++), relative, x);
                    path.lineTo(x, y);
                }
                break;
            case 'V':
                while (index < n.size()) {
                    y = value(n.get(index++), relative, y);
                    path.lineTo(x, y);
                }
                break;
            case 'C':
                while (index + 5 < n.size()) {
                    float c1x = value(n.get(index++), relative, x);
                    float c1y = value(n.get(index++), relative, y);
                    float c2x = value(n.get(index++), relative, x);
                    float c2y = value(n.get(index++), relative, y);
                    x = value(n.get(index++), relative, x);
                    y = value(n.get(index++), relative, y);
                    path.curveTo(c1x, c1y, c2x, c2y, x, y);
                    controlX = c2x;
                    controlY = c2y;
                }
                break;
            case 'Q':
                while (index + 3 < n.size()) {
                    float cx = value(n.get(index++), relative, x);
                    float cy = value(n.get(index++), relative, y);
                    x = value(n.get(index++), relative, x);
                    y = value(n.get(index++), relative, y);
                    path.quadTo(cx, cy, x, y);
                    controlX = cx;
                    controlY = cy;
                }
                break;
            case 'Z':
                path.closePath();
                x = startX;
                y = startY;
                break;
            default:
                break;
        }
        return new Object[]{x, y, startX, startY, controlX, controlY};
    }

    private static float value(float raw, boolean relative, float origin) {
        return relative ? origin + raw : raw;
    }
}
