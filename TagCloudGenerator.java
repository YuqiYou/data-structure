import java.util.Comparator;

import components.map.Map;
import components.map.Map2;
import components.sequence.Sequence;
import components.sequence.Sequence2L;
import components.set.Set;
import components.set.Set2;
import components.simplereader.SimpleReader;
import components.simplereader.SimpleReader1L;
import components.simplewriter.SimpleWriter;
import components.simplewriter.SimpleWriter1L;
import components.sortingmachine.SortingMachine;
import components.sortingmachine.SortingMachine2;

/**
 * Put a short phrase describing the program here.
 *
 * @author Yuqi You, Eric Young
 *
 */
public final class TagCloudGenerator {

    /**
     * Definition of separators.
     */
    private static final String SEPARATORS = " \t\n\r,-.!?[]';:/()*`\"=-\\|+&^%$#@";

    /**
     * Private constructor so this utility class cannot be instantiated.
     */
    private TagCloudGenerator() {
    }

    /**
     * Returns the first "word" (maximal length string of characters not in
     * {@code SEPARATORS}) or "separator string" (maximal length string of
     * characters in {@code SEPARATORS}) in the given {@code text} starting at
     * the given {@code position}.
     *
     * @param text
     *            the {@code String} from which to get the word or separator
     *            string
     * @param position
     *            the starting index
     * @return the first word or separator string found in {@code text} starting
     *         at index {@code position}
     * @requires 0 <= position < |text|
     * @ensures <pre>
     * nextWordOrSeparator =
     *   text[position, position + |nextWordOrSeparator|)  and
     * if entries(text[position, position + 1)) intersection entries(SEPARATORS) = {}
     * then
     *   entries(nextWordOrSeparator) intersection entries(SEPARATORS) = {}  and
     *   (position + |nextWordOrSeparator| = |text|  or
     *    entries(text[position, position + |nextWordOrSeparator| + 1))
     *      intersection entries(SEPARATORS) /= {})
     * else
     *   entries(nextWordOrSeparator) is subset of entries(SEPARATORS)  and
     *   (position + |nextWordOrSeparator| = |text|  or
     *    entries(text[position, position + |nextWordOrSeparator| + 1))
     *      is not subset of entries(SEPARATORS))
     * </pre>
     */
    private static String nextWordOrSeparator(String text, int position) {
        assert text != null : "Violation of: text is not null";
        assert 0 <= position : "Violation of: 0 <= position";
        assert position < text.length() : "Violation of: position < |text|";

        Set<Character> sep = new Set2<>();
        for (int i = 0; i < SEPARATORS.length(); i++) {
            if (!sep.contains(SEPARATORS.charAt(i))) {
                sep.add(SEPARATORS.charAt(i));
            }
        }

        int endIndex = position;
        boolean isSep = false;
        while (endIndex < text.length() && !isSep) {
            for (Character c : sep) {
                if (text.charAt(endIndex) == c) {
                    isSep = true;
                    if (endIndex > position) {
                        // This condition indicates what returns is a word.
                        endIndex--;
                        /*
                         * Since the index is now on the position of the
                         * separator and the index will increase by 1, we need
                         * to subtract 1 from the index in advance to ensure the
                         * content before the separator will be returned.
                         */
                    }
                }
            }
            endIndex++;
        }
        return text.substring(position, endIndex);
    }

    /**
     * Returns the {@code Map} that maps words to counts.
     *
     * @param text
     *            the {@code String} from which to get the words and their
     *            corresponding counts
     * @return the {@code Map} that stores the words as keys and the
     *         corresponding counts as values.
     * @ensures <pre> [the {@code Map} contains all words from the {@code text}.]
     *          and [Each word is associated with the times it appear in the
     *          {@code text}.]
     * </pre>
     */
    private static Map<String, Integer> processText(String text) {
        Map<String, Integer> m = new Map2<>();

        int position = 0;
        while (position < text.length()) {
            String word = nextWordOrSeparator(text, position).toLowerCase();

            if (SEPARATORS.indexOf(word) == -1) { // It is a word.
                position += word.length();
                int count = 1;
                if (m.hasKey(word)) {
                    Map.Pair<String, Integer> p = m.remove(word);
                    count = p.value();
                    m.add(p.key(), ++count);
                } else {
                    m.add(word, count);
                }
            } else { // It is not a word.
                position++;
            }

        }
        return m;
    }

    /**
     * Compare {@code Value}s in decreasing order.
     */
    private static class ValueOrder
            implements Comparator<Map.Pair<String, Integer>> {
        @Override
        public int compare(Map.Pair<String, Integer> p1,
                Map.Pair<String, Integer> p2) {
            return p2.value().compareTo(p1.value());
        }
    }

    /**
     * Compare {@code Key}s in alphabetical order.
     */
    private static class KeyOrder
            implements Comparator<Map.Pair<String, Integer>> {
        @Override
        public int compare(Map.Pair<String, Integer> p1,
                Map.Pair<String, Integer> p2) {
            return p1.key().compareToIgnoreCase(p2.key());
        }
    }

    /**
     * Returns the {@code SortingMachine} with counts in decreasing order.
     *
     * @param map
     *            the initial unsorted {@code Map} that maps words to counts
     * @return the {@code SortingMachine} with the words arranged in decreasing
     *         order of counts
     * @ensures <pre> [the set of the pairs in the returned
     *          {@code SortingMachine} equals the set of the pairs in the {@code Map}] and
     *          [the pairs are arranged in decreasing order of  the values] and
     *          map = #map
     *</pre>
     */
    private static SortingMachine<Map.Pair<String, Integer>> sortCounts(
            Map<String, Integer> map) {

        Map<String, Integer> temp = map.newInstance();
        temp.transferFrom(map);
        Comparator<Map.Pair<String, Integer>> ci = new ValueOrder();
        SortingMachine<Map.Pair<String, Integer>> sort = new SortingMachine2<>(
                ci);

        while (temp.size() > 0) {
            Map.Pair<String, Integer> p = temp.removeAny();
            sort.add(p);
            map.add(p.key(), p.value());
        }
        sort.changeToExtractionMode();

        return sort;
    }

    /**
     * Returns the {@code SortingMachine} with words in alphabetical order.
     *
     * @param sorted
     *            the {@code SortingMachine} that arranges words in decreasing
     *            order of counts
     * @param num
     *            N, the number of words to be included in the tag cloud
     * @updates sorted
     * @return the {@code SortingMachine} that arranges the N most frequent
     *         words with their counts in alphabetical order
     * @requires num > 0 and [sorted has all the words in decreasing order of
     *           counts]
     * @ensures <pre> [the returned {@ SortingMachine} contains the N most frequent
     *          words in alphabetical order] and
     *          [the set of pairs of #sorted = the set of pairs of sorted  +
     *          the set of pairs in the returned {@code SortingMachine}]
     *</pre>
     */
    private static SortingMachine<Map.Pair<String, Integer>> sortWords(
            SortingMachine<Map.Pair<String, Integer>> sorted, int num) {

        Comparator<Map.Pair<String, Integer>> ci = new KeyOrder();
        SortingMachine<Map.Pair<String, Integer>> sort = new SortingMachine2<>(
                ci);
        for (int i = 0; i < num; i++) {
            sort.add(sorted.removeFirst());
        }
        sort.changeToExtractionMode();

        return sort;
    }

    /**
     * Returns the {@code Map} that maps the words to the font sizes according
     * to decreasing order of word frequency.
     *
     * @param sorted
     *            the {@code SortingMachine} that arranges words in decreasing
     *            order of counts
     * @param num
     *            N, the number of words to be included in the tag cloud
     * @updates sorted
     * @return the {@code Map} contains the N most frequent words with
     *         corresponding font sizes
     * @requires num > 0 and [sorted has all the words in decreasing order of
     *           counts]
     * @ensures <pre> [the returned {@code Map} contains the N most frequent
     *          words with font sizes that are proportional to the word
     *          occurrences] and
     *          [the set of pairs of #sorted = the set of pairs of sorted  +
     *          the set of pairs in the returned {@code Map}]
     *</pre>
     */
    private static Map<String, Integer> fontSize(
            SortingMachine<Map.Pair<String, Integer>> sorted, int num) {

        Map<String, Integer> temp = new Map2<>();
        Sequence<Integer> counts = new Sequence2L<>();
        int maxCount = 0;
        int minCount = 0;
        int font = 11;

        /*
         * Set the standard for the font sizes, i.e., the max font size
         * corresponds to the largest count, and the min to the smallest.
         */
        for (int i = 0; i < num; i++) {
            Map.Pair<String, Integer> p = sorted.removeFirst();
            if (i == 0) {
                maxCount = p.value();
            }
            if (i == num - 1) {
                minCount = p.value();
            }
            counts.add(i, p.value());
            temp.add(p.key(), p.value());
        }

        // Replace the values in the map with the font sizes.
        Map<String, Integer> m = new Map2<>();
        while (temp.size() > 0) {
            Map.Pair<String, Integer> p = temp.removeAny();
            if (maxCount == minCount) {
                m.add(p.key(), font);
            } else {
                font = 48 - (maxCount - p.value()) * 38 / (maxCount - minCount);
                if (font < 11) {
                    font = 11;
                }
                m.add(p.key(), font);
                // 38 is the number of font sizes, which is (48 - 11) / 1 + 1.
            }
        }
        return m;
    }

    /**
     * Prints header.
     *
     * @param input
     *            the path or name for the input file
     * @param output
     *            the output stream
     * @param num
     *            N, the number of words to be included in the tag cloud
     * @updates output.content
     * @requires out.is_open
     * @ensures <pre> the header of the HTML file is output in the correct format,
     *          containing in the title the input file path or name and the
     *          number of the words included
     *</pre>
     */
    private static void printHeader(String input, SimpleWriter output,
            int num) {

        output.print("<html>\n<head>\n<title>Top " + num + " words in " + input
                + "</title>\n");
        output.print(
                "<link href=\"tagcloud.css\" rel=\"stylesheet\" type=\"text/css\">\n"
                        + "</head>\n");
        output.print("<body>\n<h2>Top " + num + " words in " + input
                + "</h2>\n<hr>\n<div class=\"cdiv\">\n<p class=\"cbox\">\n");
    }

    /**
     * Prints each item.
     *
     * @param output
     *            the output stream
     * @param sorted
     *            the {@code SortingMachine} that contains the N most frequent
     *            words in alphabetical order
     * @param fonts
     *            the {@code Map} that contains the N most frequent words with
     *            font sizes that are proportional to the word occurrences
     * @updates output.content
     * @requires out.is_open
     * @ensures <pre> [the body of the HTML file is output in the correct format] and
     * [the words are printed in alphebetical order] and
     * [the font size of each words is proportional to its occurrences in the input text]
     *</pre>
     */
    private static void printWords(SimpleWriter output,
            SortingMachine<Map.Pair<String, Integer>> sorted,
            Map<String, Integer> fonts) {

        while (sorted.size() > 0) {
            Map.Pair<String, Integer> p = sorted.removeFirst();
            output.print("<span style=\"cursor:default\" class=\"f");
            output.print(fonts.value(p.key()) + "\" title=\"count: " + p.value()
                    + "\">" + p.key() + "</span>\n");
        }

    }

    /**
     * Prints footer.
     *
     * @param output
     *            the output stream
     * @updates output.content
     * @requires out.is_open
     * @ensures the footer of the HTML file is output in the correct format
     */
    private static void printFooter(SimpleWriter output) {
        output.print("</p>\n</div>\n</body>\n</html>\n");
    }

    /**
     * Main method.
     *
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        SimpleReader in = new SimpleReader1L();
        SimpleWriter out = new SimpleWriter1L();

        // Get the input file path or name
        out.print("Input the path or name for the input file: ");
        String input = in.nextLine();

        // Get N, the number of words to be included in tag cloud
        out.print(
                "\nInput N>0 as the number of words to be generated in the tag cloud: ");
        int n = in.nextInteger();

        // Get the output file path or name
        out.print("\nInput the path or name for the output file: ");
        String p2 = in.nextLine();
        SimpleWriter output = new SimpleWriter1L(p2);
        out.println();

        // Generate the text from the input file
        SimpleReader txt = new SimpleReader1L(input);
        String text = "";
        while (!txt.atEOS()) {
            String t = txt.nextLine();
            text = text.concat(t);
            text = text.concat(" ");
            /*
             * This not only prevents errors but also connects one line and
             * another with space.
             */
        }
        txt.close();

        // Generate the map with the words and their counts
        Map<String, Integer> map = processText(text);

        // Get the N most frequent words in alphabetical order
        SortingMachine<Map.Pair<String, Integer>> sort2 = sortWords(
                sortCounts(map), n);

        // Generate the map with the words and their font sizes
        Map<String, Integer> fonts = fontSize(sortCounts(map), n);

        // Print the header
        printHeader(input, output, n);

        // Print the words
        printWords(output, sort2, fonts);

        // Print the footer
        printFooter(output);

        out.print("Process completed.");

        in.close();
        output.close();
        out.close();
    }

}
