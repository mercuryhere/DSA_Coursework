package q1;

import java.util.*;

public class WordBreakAllSentences {

    // Returns all possible sentences that can be formed by inserting spaces
    // such that every word exists in the dictionary.
    // Uses DFS + memoization to avoid repeated work.
    // Time: can be exponential in worst-case (output size), but memo makes it practical.
    public static List<String> wordBreakAll(String s, Set<String> dict) {
        if (s == null) return Collections.emptyList();
        Map<Integer, List<String>> memo = new HashMap<>();
        return dfs(s, 0, dict, memo);
    }

    private static List<String> dfs(String s, int start, Set<String> dict, Map<Integer, List<String>> memo) {
        if (memo.containsKey(start)) return memo.get(start);

        List<String> res = new ArrayList<>();

        // If we reached end, one valid sentence = empty tail
        if (start == s.length()) {
            res.add("");
            memo.put(start, res);
            return res;
        }

        // Try all possible word breaks starting at 'start'
        for (int end = start + 1; end <= s.length(); end++) {
            String word = s.substring(start, end);
            if (!dict.contains(word)) continue;

            List<String> tails = dfs(s, end, dict, memo);
            for (String tail : tails) {
                // If tail is empty, sentence ends here
                if (tail.isEmpty()) res.add(word);
                else res.add(word + " " + tail);
            }
        }

        memo.put(start, res);
        return res;
    }

    // Quick local tests
    public static void main(String[] args) {
        String s1 = "catsanddog";
        Set<String> dict1 = new HashSet<>(Arrays.asList("cat", "cats", "and", "sand", "dog"));
        List<String> out1 = wordBreakAll(s1, dict1);
        System.out.println("Input: " + s1);
        System.out.println("Expected 2 sentences, got: " + out1.size());
        for (String sent : out1) System.out.println(" - " + sent);

        System.out.println();

        String s2 = "pineapplepenapple";
        Set<String> dict2 = new HashSet<>(Arrays.asList("apple", "pen", "applepen", "pine", "pineapple"));
        List<String> out2 = wordBreakAll(s2, dict2);
        System.out.println("Input: " + s2);
        System.out.println("Expected 3 sentences, got: " + out2.size());
        for (String sent : out2) System.out.println(" - " + sent);

        System.out.println();

        String s3 = "aaaaaaa";
        Set<String> dict3 = new HashSet<>(Arrays.asList("a", "aa", "aaa", "aaaa"));
        List<String> out3 = wordBreakAll(s3, dict3);
        System.out.println("Input: " + s3);
        System.out.println("Got sentences: " + out3.size());
        // print a few only
        for (int i = 0; i < Math.min(10, out3.size()); i++) {
            System.out.println(" - " + out3.get(i));
        }
    }
}