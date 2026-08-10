package ru.fluxvisuals.vse.utils.client.text;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ServerPrefix {

    final String[] RW_MAP = new String[] {
            "ꔀꔁꔂꔃ",
            "ꔄꔅꔆꔇ",
            "ꔈꔉꔐꔑ",
            "ꔒꔓꔔꔕ",
            "ꔖꔗꔘꔙ",
            "ꔠꔡꔢꔣ",
            "ꔤꔥꔦꔧ",
            "ꔨꔩꔰꔱ",
            "ꔲꔳꔴꔵ",
            "ꔶꔷꔸꔹ",
            "ꕀꕁꕂꕃ",
            "ꕄꕅꕆꕇ",
            "ꕈꕉꕐꕑ",
            "ꕒꕓꕔꕕ",
            "ꕖꕗꕘꕙ",
            "ꕠꕡꕢꕣ"
    };

    public int[] rwFrom(String c) {
        int x = -1, y = -1;
        boolean found = false;
        for (y = 0; y < RW_MAP.length; y++) {
            boolean stop = false;
            for (x = 0; x < RW_MAP[y].length(); x++) {
                if (RW_MAP[y].charAt(x) == c.charAt(0)) {
                    stop = true;
                    found = true;
                    break;
                }
            }
            if (stop) break;
        }
        if (!found) {
            x = -1; y = -1;
        }
        return new int[] { x, y };
    }
}
