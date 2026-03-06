package q2;

public class MaxPathSumBinaryTree {

    // Tree node definition
    public static class Node{
        int val;
        Node left, right;
        Node(int v) { this.val = v; }
    }

    private static int globalMax;

    // Returns maximum path sum in the tree.
    // Time: O(n), Space: O(h) recursion stack.
    public static int maxPathSum(Node root) {
        globalMax = Integer.MIN_VALUE;
        dfs(root);
        return globalMax;
    }

    // Returns maximum "gain" from this node to its parent:
    // node.val + max(0, leftGain, rightGain)
    private static int dfs(Node node) {
        if (node == null) return 0;

        int leftGain = Math.max(0, dfs(node.left));
        int rightGain = Math.max(0, dfs(node.right));

        // Best path through this node (could connect left + node + right)
        int bestThroughNode = node.val + leftGain + rightGain;

        // Update global answer
        globalMax = Math.max(globalMax, bestThroughNode);

        // Return best single-side path to parent
        return node.val + Math.max(leftGain, rightGain);
    }

    // Quick local tests
    public static void main(String[] args) {
        // Example 1: [-10,9,20,null,null,15,7] => 42 (15+20+7)
        Node root1 = new Node(-10);
        root1.left = new Node(9);
        root1.right = new Node(20);
        root1.right.left = new Node(15);
        root1.right.right = new Node(7);
        System.out.println("Expected 42, got: " + maxPathSum(root1));

        // Example 2: [1,2,3] => 6
        Node root2 = new Node(1);
        root2.left = new Node(2);
        root2.right = new Node(3);
        System.out.println("Expected 6, got: " + maxPathSum(root2));

        // Example 3: all negative: [-3] => -3
        Node root3 = new Node(-3);
        System.out.println("Expected -3, got: " + maxPathSum(root3));

        // Example 4: [-2, -1] => -1
        Node root4 = new Node(-2);
        root4.left = new Node(-1);
        System.out.println("Expected -1, got: " + maxPathSum(root4));
    }
}