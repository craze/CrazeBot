package net.bashtech.geobot;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public final class PropertiesFile {

    public static final String DELIMITER = "│";
    public static final String DELIMITER2 = "≡";

    private static final Logger log = Logger.getLogger("GeoBot");
    private String fileName;
    private List<String> lines = new ArrayList<String>();
    private Map<String, String> props = new HashMap<String, String>();

    private Map<String, String> cached = new HashMap<String, String>();

    /**
     * Creates or opens a properties file using specified filename
     *
     * @param fileName
     */
    public PropertiesFile(String fileName) {
        this.fileName = fileName;
        File file = new File(fileName);

        if (file.exists()) {
            try {
                load();
            } catch (IOException ex) {
                log.severe("[PropertiesFile] Unable to load " + fileName + "!");
            }
        } else {
            save();
        }
    }

    /**
     * The loader for property files, it reads the file as UTF8 or converts the string into UTF8.
     * Used for simple runthrough's, loading, or reloading of the file.
     *
     * @throws IOException
     */
    public void load() throws IOException {
        BufferedReader reader;
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.fileName), "UTF8"));
        String line;

        // Clear the file & unwritten properties
        lines.clear();
        props.clear();

        // Begin reading the file.
        while ((line = reader.readLine()) != null) {
            line = new String(line.getBytes(), "UTF-8");
            char c = 0;
            int pos = 0;

            while (pos < line.length() && Character.isWhitespace(c = line.charAt(pos))) {
                pos++;
            }

            if ((line.length() - pos) == 0 || line.charAt(pos) == '#' || line.charAt(pos) == '!') {
                lines.add(line);
                continue;
            }

            int start = pos;
            //boolean needsEscape = line.indexOf('\\', pos) != -1; // CHANGED TO FALSE
            boolean needsEscape = false;
            StringBuffer key = needsEscape ? new StringBuffer() : null;

            while (pos < line.length() && !Character.isWhitespace(c = line.charAt(pos++)) && c != '=' && c != ':') {
                if (needsEscape && c == '\\') {
                    if (pos == line.length()) {
                        line = reader.readLine();

                        if (line == null) {
                            line = "";
                        }

                        pos = 0;

                        while (pos < line.length() && Character.isWhitespace(c = line.charAt(pos))) {
                            pos++;
                        }
                    } else {
                        c = line.charAt(pos++);

                        switch (c) {
                            case 'n':
                                key.append('\n');
                                break;
                            case 't':
                                key.append('\t');
                                break;
                            case 'r':
                                key.append('\r');
                                break;
                            case 'u':
                                if (pos + 4 <= line.length()) {
                                    char uni = (char) Integer.parseInt(line.substring(pos, pos + 4), 16);
                                    key.append(uni);
                                    pos += 4;
                                }

                                break;
                            default:
                                key.append(c);
                                break;
                        }
                    }
                } else if (needsEscape) {
                    key.append(c);
                }
            }

            boolean isDelim = (c == ':' || c == '=');
            String keyString;

            if (needsEscape) {
                keyString = key.toString();
            } else if (isDelim || Character.isWhitespace(c)) {
                keyString = line.substring(start, pos - 1);
            } else {
                keyString = line.substring(start, pos);
            }

            while (pos < line.length() && Character.isWhitespace(c = line.charAt(pos))) {
                pos++;
            }

            if (!isDelim && (c == ':' || c == '=')) {
                pos++;

                while (pos < line.length() && Character.isWhitespace(c = line.charAt(pos))) {
                    pos++;
                }
            }

            // Short-circuit if no escape chars found.
            if (!needsEscape) {
                lines.add(line);
                continue;
            }

            // Escape char found so iterate through the rest of the line.
            StringBuilder element = new StringBuilder(line.length() - pos);
            while (pos < line.length()) {
                c = line.charAt(pos++);
                if (c == '\\') {
                    if (pos == line.length()) {
                        line = reader.readLine();

                        if (line == null) {
                            break;
                        }

                        pos = 0;
                        while (pos < line.length() && Character.isWhitespace(c = line.charAt(pos))) {
                            pos++;
                        }
                        element.ensureCapacity(line.length() - pos + element.length());
                    } else {
                        c = line.charAt(pos++);
                        switch (c) {
                            case 'n':
                                element.append('\n');
                                break;
                            case 't':
                                element.append('\t');
                                break;
                            case 'r':
                                element.append('\r');
                                break;
                            case 'u':
                                if (pos + 4 <= line.length()) {
                                    char uni = (char) Integer.parseInt(line.substring(pos, pos + 4), 16);
                                    element.append(uni);
                                    pos += 4;
                                }
                                break;
                            default:
                                element.append(c);
                                break;
                        }
                    }
                } else {
                    element.append(c);
                }
            }
            lines.add(keyString + "=" + element.toString());
        }

        reader.close();
    }

    /**
     * Writes out the <code>key=value</code> properties that were changed into
     * a .[properties] file in UTF8.
     */
    public void save() {
        BufferedWriter os = null;

        try {
            os = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF8"));
        } catch (UnsupportedEncodingException ex) {
            log.severe("[PropertiesFile] Unable to write to file " + fileName + "!");
        } catch (FileNotFoundException ex) {
            log.severe("[PropertiesFile] Unable to find file " + fileName + "!");
        }


        // Keep track of properties that were set
        List<String> usedProps = new ArrayList<String>();

        try {
            for (String line : this.lines) {
                if (line.trim().length() == 0) {
                    os.write(line);
                    os.newLine();
                    continue;
                }

                if (line.charAt(0) == '#') {
                    os.write(line);
                    os.newLine();
                    continue;
                }

                if (line.contains("=")) {
                    int delimPosition = line.indexOf('=');
                    String key = line.substring(0, delimPosition).trim();

                    if (this.props.containsKey(key)) {
                        String value = this.props.get(key);
                        os.write(key + "=" + value);
                        os.newLine();
                        usedProps.add(key);
                    } else {
                        os.write(line);
                        os.newLine();
                    }
                } else {
                    os.write(line);
                    os.newLine();
                }
            }

            // Add any new properties
            for (Map.Entry<String, String> entry : this.props.entrySet()) {
                if (!usedProps.contains(entry.getKey())) {
                    os.write(entry.getKey() + "=" + entry.getValue());
                    os.newLine();
                }
            }

            // Exit that stream
            os.close();
        } catch (IOException ex) {
            log.severe("[PropertiesFile] Unable to write to file " + fileName + "!");
        }

        // Reload
        try {
            lines.clear();
            props.clear();
            this.load();
        } catch (IOException ex) {
            log.severe("[PropertiesFile] Unable to load " + fileName + "!");
        }
    }

    /**
     * Returns a Map of all <code>key=value</code> properties in the file as <code>&lt;key (java.lang.String), value (java.lang.String)></code>
     * <br /><br />
     * Example:
     * <blockquote><pre>
     * PropertiesFile settings = new PropertiesFile("settings.properties");
     * Map<String, String> mappedSettings;
     * <p/>
     * try {
     * 	 mappedSettings = settings.returnMap();
     * } catch (Exception ex) {
     * 	 log.info("Failed mapping settings.properties");
     * }
     * </pre></blockquote>
     *
     * @return <code>map</code> - Simple Map HashMap of the entire <code>key=value</code> as <code>&lt;key (java.lang.String), value (java.lang.String)></code>
     * @throws Exception If the properties file doesn't exist.
     */
    public Map<String, String> returnMap() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader reader = new BufferedReader(new FileReader(this.fileName));
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }

            if (line.charAt(0) == '#') {
                continue;
            }

            if (line.contains("=")) {
                int delimPosition = line.indexOf('=');
                String key = line.substring(0, delimPosition).trim();
                String value = line.substring(delimPosition + 1).trim();
                map.put(key, value);
            } else {
                continue;
            }
        }

        reader.close();
        return map;
    }

    /**
     * Checks to see if the .[properties] file contains the given <code>key</code>.
     *
     * @param var The key we are going to be checking the existance of.
     * @return <code>Boolean</code> - True if the <code>key</code> exists, false if it cannot be found.
     */
    public boolean containsKey(String var) {
        //Start caching
        if (cached.containsKey(var))
            return true;
        //End caching

        for (String line : this.lines) {
            if (line.trim().length() == 0) {
                continue;
            }

            if (line.charAt(0) == '#') {
                continue;
            }

            if (line.contains("=")) {
                int delimPosition = line.indexOf('=');
                String key = line.substring(0, delimPosition);

                if (key.equals(var)) {
                    return true;
                }
            } else {
                continue;
            }
        }

        return false;
    }

    public String getProperty(String var) {
        //Start caching
        if (cached.containsKey(var))
            return cached.get(var);
        //End caching

        for (String line : this.lines) {
            if (line.trim().length() == 0) {
                continue;
            }
            if (line.charAt(0) == '#') {
                continue;
            }

            if (line.contains("=")) {
                int delimPosition = line.indexOf('=');
                String key = line.substring(0, delimPosition).trim();
                String value = line.substring(delimPosition + 1);

                if (key.equals(var)) {
                    return value;
                }
            } else {
                continue;
            }
        }

        return "";
    }

    /**
     * Remove a key from the file if it exists.
     * This will save() which will invoke a load() on the file.
     *
     * @param var The <code>key</code> that will be removed from the file
     * @see #save()
     */
    public void removeKey(String var) {
        Boolean changed = false;

        if (this.props.containsKey(var)) {
            this.props.remove(var);
            changed = true;
        }

        // Use an iterator to prevent ConcurrentModification exceptions
        Iterator<String> it = this.lines.listIterator();
        while (it.hasNext()) {
            String line = it.next();

            if (line.trim().length() == 0) {
                continue;
            }

            if (line.charAt(0) == '#') {
                continue;
            }

            if (line.contains("=")) {
                int delimPosition = line.indexOf('=');
                String key = line.substring(0, delimPosition).trim();

                if (key.equals(var)) {
                    it.remove();
                    changed = true;
                }
            } else {
                continue;
            }
        }

        // Save on change
        if (changed) {
            save();
        }
    }

    /**
     * Checks the existance of a <code>key</code>.
     *
     * @param key The <code>key</code> in question of existance.
     * @return <code>Boolean</code> - True for existance, false for <code>key</code> found.
     * @see #containsKey(java.lang.String)
     */
    public boolean keyExists(String key) {
        try {
            return (this.containsKey(key)) ? true : false;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Returns the value of the <code>key</code> given as a <code>String</code>,
     * however we do not set a string if no <code>key</code> is found.
     *
     * @param key The <code>key</code> we will retrieve the property from, if no <code>key</code> is found default to "" or empty.
     * @see #getProperty(java.lang.String)
     */
    public String getString(String key) {
        if (this.containsKey(key)) {
            return this.getProperty(key);
        }

        return "";
    }

    /**
     * Save the value given as a <code>String</code> on the specified key.
     *
     * @param key   The <code>key</code> that we will be addressing the <code>value</code> to.
     * @param value The <code>value</code> we will be setting inside the <code>.[properties]</code> file.
     * @see #save()
     */
    public void setString(String key, String value) {
        //Start caching
        cached.put(key, value);
        //End caching
        props.put(key, value);

        save();
    }

    /**
     * Returns the value of the <code>key</code> given in a Integer,
     * however we do not set a string if no <code>key</code> is found.
     *
     * @param key The <code>key</code> we will retrieve the property from, if no <code>key</code> is found default to 0
     * @see #getProperty(String var)
     */
    public int getInt(String key) {
        if (this.containsKey(key)) {
            return Integer.parseInt(this.getString(key));
        }

        return 0;
    }

    /**
     * Save the value given as a <code>int</code> on the specified key.
     *
     * @param key   The <code>key</code> that we will be addressing the <code>value</code> to.
     * @param value The <code>value</code> we will be setting inside the <code>.[properties]</code> file.
     * @see #save()
     */
    public void setInt(String key, int value) {
        this.setString(key, String.valueOf(value));
    }

    /**
     * Returns the value of the <code>key</code> given in a Double,
     * however we do not set a string if no <code>key</code> is found.
     *
     * @param key The <code>key</code> we will retrieve the property from, if no <code>key</code> is found default to 0.0
     * @see #getProperty(String var)
     */
    public double getDouble(String key) {
        if (this.containsKey(key)) {
            return Double.parseDouble(this.getString(key));
        }

        return 0;
    }

    /**
     * Save the value given as a <code>double</code> on the specified key.
     *
     * @param key   The <code>key</code> that we will be addressing the <code>value</code> to.
     * @param value The <code>value</code> we will be setting inside the <code>.[properties]</code> file.
     * @see #save()
     */
    public void setDouble(String key, double value) {
        this.setString(key, String.valueOf(value));
    }

    /**
     * Returns the value of the <code>key</code> given in a Long,
     * however we do not set a string if no <code>key</code> is found.
     *
     * @param key The <code>key</code> we will retrieve the property from, if no <code>key</code> is found default to 0L
     * @see #getProperty(String var)
     */
    public long getLong(String key) {
        if (this.containsKey(key)) {
            return Long.parseLong(this.getString(key));
        }

        return 0;
    }


    /**
     * Save the value given as a <code>long</code> on the specified key.
     *
     * @param key   The <code>key</code> that we will be addressing the <code>value</code> to.
     * @param value The <code>value</code> we will be setting inside the <code>.[properties]</code> file.
     * @see #save()
     */
    public void setLong(String key, long value) {
        this.setString(key, String.valueOf(value));
    }

    /**
     * Returns the value of the <code>key</code> given in a Boolean,
     * however we do not set a string if no <code>key</code> is found.
     *
     * @param key The <code>key</code> we will retrieve the property from, if no <code>key</code> is found default to false
     * @see #getProperty(String var)
     */
    public boolean getBoolean(String key) {
        if (this.containsKey(key)) {
            return Boolean.parseBoolean(this.getString(key));
        }

        return false;
    }

    /**
     * Save the value given as a <code>boolean</code> on the specified key.
     *
     * @param key   The <code>key</code> that we will be addressing the <code>value</code> to.
     * @param value The <code>value</code> we will be setting inside the <code>.[properties]</code> file.
     * @see #save()
     */
    public void setBoolean(String key, boolean value) {
        this.setString(key, String.valueOf(value));
    }

    public List<String> getList(String key, List<String> returnObject) {
        if (this.containsKey(key)) {
            String raw = this.getProperty(key);
            String[] rawSplit = raw.split(PropertiesFile.DELIMITER);
            for (String p : rawSplit) {
                returnObject.add(p);
            }
        }

        return returnObject;
    }

    public void setList(String key, List<?> list) {
        String outString = "";
        String delim = "";

        for (Object p : list) {
            String strValue = String.valueOf(p);
            strValue = strValue.replaceAll(PropertiesFile.DELIMITER, "");
            outString += delim + strValue;
            delim = PropertiesFile.DELIMITER;
        }

        System.out.println(outString);
        this.setString(key, outString);
    }

    public Map<String, String> getMap(String key) {
        Map<String, String> map = new HashMap<String, String>();

        if (this.containsKey(key)) {
            String raw = this.getProperty(key);
            String[] rawSplit = raw.split(PropertiesFile.DELIMITER);
            for (String p : rawSplit) {
                String[] rawSplitInner = p.split(PropertiesFile.DELIMITER2);
                if (rawSplitInner.length == 2) {
                    map.put(rawSplitInner[0], rawSplitInner[1]);
                }
            }
        }

        return map;
    }

    public void setMap(String key, Map<?, ?> map) {
        String outString = "";
        String delim = "";
        Iterator it = map.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            String pair = String.valueOf(pairs.getKey()).replaceAll(PropertiesFile.DELIMITER, "").replaceAll(PropertiesFile.DELIMITER2, "") + PropertiesFile.DELIMITER2 +
                    String.valueOf(pairs.getValue()).replaceAll(PropertiesFile.DELIMITER, "").replaceAll(PropertiesFile.DELIMITER2, "");
            outString += delim + pair;
            delim = PropertiesFile.DELIMITER;
        }

        System.out.println(outString);
        this.setString(key, outString);
    }

    public Set<String> getSet(String key) {
        Set<String> returnObject = new HashSet<String>();

        if (this.containsKey(key)) {
            String raw = this.getProperty(key);
            String[] rawSplit = raw.split(PropertiesFile.DELIMITER);
            for (String p : rawSplit) {
                returnObject.add(p);
            }
        }

        return returnObject;
    }

    public void setSet(String key, Set<?> set) {
        String outString = "";
        String delim = "";

        for (Object p : set) {
            String strValue = String.valueOf(p);
            strValue = strValue.replaceAll(PropertiesFile.DELIMITER, "");
            outString += delim + strValue;
            delim = PropertiesFile.DELIMITER;
        }

        this.setString(key, outString);
    }
}
