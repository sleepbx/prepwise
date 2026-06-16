#!/usr/bin/env python3
"""Generate PrepWise CSV datasets with realistic DSA problem metadata."""
import csv
import os
import random

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "datasets")
os.makedirs(OUT, exist_ok=True)

TOPICS = ["ARRAY", "STRING", "LINKED_LIST", "STACK", "QUEUE", "TREE", "GRAPH", "HEAP",
          "GREEDY", "DP", "BACKTRACKING", "BINARY_SEARCH"]
PATTERNS = ["HASHING", "TWO_POINTER", "SLIDING_WINDOW", "PREFIX_SUM", "SORTING",
            "BINARY_SEARCH_PATTERN", "DFS", "BFS", "TOPOLOGICAL_SORT", "UNION_FIND",
            "SHORTEST_PATH", "BACKTRACKING_PATTERN", "MONOTONIC_STACK", "TOP_K",
            "INTERVALS", "KNAPSACK", "LIS", "RECURSION", "GREEDY_PATTERN"]
DIFFS = ["EASY", "MEDIUM", "HARD"]

BLIND_75 = [
    ("two-sum", "Two Sum", "EASY", "ARRAY", "HASHING"),
    ("best-time-to-buy-and-sell-stock", "Best Time to Buy and Sell Stock", "EASY", "ARRAY", "SLIDING_WINDOW"),
    ("contains-duplicate", "Contains Duplicate", "EASY", "ARRAY", "HASHING"),
    ("product-of-array-except-self", "Product of Array Except Self", "MEDIUM", "ARRAY", "PREFIX_SUM"),
    ("maximum-subarray", "Maximum Subarray", "MEDIUM", "ARRAY", "SLIDING_WINDOW"),
    ("maximum-product-subarray", "Maximum Product Subarray", "MEDIUM", "ARRAY", "DP"),
    ("find-minimum-in-rotated-sorted-array", "Find Minimum in Rotated Sorted Array", "MEDIUM", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("search-in-rotated-sorted-array", "Search in Rotated Sorted Array", "MEDIUM", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("3sum", "3Sum", "MEDIUM", "ARRAY", "TWO_POINTER"),
    ("container-with-most-water", "Container With Most Water", "MEDIUM", "ARRAY", "TWO_POINTER"),
    ("sum-of-two-integers", "Sum of Two Integers", "MEDIUM", "ARRAY", "RECURSION"),
    ("number-of-1-bits", "Number of 1 Bits", "EASY", "ARRAY", "RECURSION"),
    ("counting-bits", "Counting Bits", "EASY", "ARRAY", "DP"),
    ("missing-number", "Missing Number", "EASY", "ARRAY", "HASHING"),
    ("reverse-bits", "Reverse Bits", "EASY", "ARRAY", "RECURSION"),
    ("climbing-stairs", "Climbing Stairs", "EASY", "DP", "DP"),
    ("coin-change", "Coin Change", "MEDIUM", "DP", "KNAPSACK"),
    ("longest-increasing-subsequence", "Longest Increasing Subsequence", "MEDIUM", "DP", "LIS"),
    ("longest-common-subsequence", "Longest Common Subsequence", "MEDIUM", "STRING", "DP"),
    ("word-break", "Word Break", "MEDIUM", "STRING", "DP"),
    ("combination-sum", "Combination Sum", "MEDIUM", "ARRAY", "BACKTRACKING_PATTERN"),
    ("house-robber", "House Robber", "MEDIUM", "DP", "DP"),
    ("house-robber-ii", "House Robber II", "MEDIUM", "DP", "DP"),
    ("decode-ways", "Decode Ways", "MEDIUM", "STRING", "DP"),
    ("unique-paths", "Unique Paths", "MEDIUM", "DP", "DP"),
    ("jump-game", "Jump Game", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("merge-intervals", "Merge Intervals", "MEDIUM", "ARRAY", "INTERVALS"),
    ("insert-interval", "Insert Interval", "MEDIUM", "ARRAY", "INTERVALS"),
    ("non-overlapping-intervals", "Non-overlapping Intervals", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("meeting-rooms", "Meeting Rooms", "EASY", "ARRAY", "INTERVALS"),
    ("meeting-rooms-ii", "Meeting Rooms II", "MEDIUM", "ARRAY", "TOP_K"),
    ("rotate-image", "Rotate Image", "MEDIUM", "ARRAY", "TWO_POINTER"),
    ("spiral-matrix", "Spiral Matrix", "MEDIUM", "ARRAY", "TWO_POINTER"),
    ("set-matrix-zeroes", "Set Matrix Zeroes", "MEDIUM", "ARRAY", "HASHING"),
    ("valid-anagram", "Valid Anagram", "EASY", "STRING", "HASHING"),
    ("group-anagrams", "Group Anagrams", "MEDIUM", "STRING", "HASHING"),
    ("valid-parentheses", "Valid Parentheses", "EASY", "STRING", "STACK"),
    ("min-stack", "Min Stack", "MEDIUM", "STACK", "STACK"),
    ("evaluate-reverse-polish-notation", "Evaluate Reverse Polish Notation", "MEDIUM", "STACK", "STACK"),
    ("daily-temperatures", "Daily Temperatures", "MEDIUM", "ARRAY", "MONOTONIC_STACK"),
    ("car-fleet", "Car Fleet", "MEDIUM", "ARRAY", "MONOTONIC_STACK"),
    ("largest-rectangle-in-histogram", "Largest Rectangle in Histogram", "HARD", "ARRAY", "MONOTONIC_STACK"),
    ("reverse-linked-list", "Reverse Linked List", "EASY", "LINKED_LIST", "TWO_POINTER"),
    ("merge-two-sorted-lists", "Merge Two Sorted Lists", "EASY", "LINKED_LIST", "TWO_POINTER"),
    ("linked-list-cycle", "Linked List Cycle", "EASY", "LINKED_LIST", "TWO_POINTER"),
    ("remove-nth-node-from-end-of-list", "Remove Nth Node From End of List", "MEDIUM", "LINKED_LIST", "TWO_POINTER"),
    ("reorder-list", "Reorder List", "MEDIUM", "LINKED_LIST", "TWO_POINTER"),
    ("merge-k-sorted-lists", "Merge k Sorted Lists", "HARD", "HEAP", "TOP_K"),
    ("invert-binary-tree", "Invert Binary Tree", "EASY", "TREE", "DFS"),
    ("maximum-depth-of-binary-tree", "Maximum Depth of Binary Tree", "EASY", "TREE", "DFS"),
    ("diameter-of-binary-tree", "Diameter of Binary Tree", "EASY", "TREE", "DFS"),
    ("balanced-binary-tree", "Balanced Binary Tree", "EASY", "TREE", "DFS"),
    ("same-tree", "Same Tree", "EASY", "TREE", "DFS"),
    ("subtree-of-another-tree", "Subtree of Another Tree", "EASY", "TREE", "DFS"),
    ("lowest-common-ancestor-of-a-binary-search-tree", "Lowest Common Ancestor of a BST", "MEDIUM", "TREE", "DFS"),
    ("binary-tree-level-order-traversal", "Binary Tree Level Order Traversal", "MEDIUM", "TREE", "BFS"),
    ("validate-binary-search-tree", "Validate Binary Search Tree", "MEDIUM", "TREE", "DFS"),
    ("kth-smallest-element-in-a-bst", "Kth Smallest Element in a BST", "MEDIUM", "TREE", "DFS"),
    ("construct-binary-tree-from-preorder-and-inorder-traversal", "Construct Binary Tree from Preorder and Inorder", "MEDIUM", "TREE", "DFS"),
    ("binary-tree-maximum-path-sum", "Binary Tree Maximum Path Sum", "HARD", "TREE", "DFS"),
    ("serialize-and-deserialize-binary-tree", "Serialize and Deserialize Binary Tree", "HARD", "TREE", "DFS"),
    ("implement-trie-prefix-tree", "Implement Trie (Prefix Tree)", "MEDIUM", "STRING", "RECURSION"),
    ("design-add-and-search-words-data-structure", "Design Add and Search Words Data Structure", "MEDIUM", "STRING", "DFS"),
    ("word-search", "Word Search", "MEDIUM", "ARRAY", "BACKTRACKING_PATTERN"),
    ("word-search-ii", "Word Search II", "HARD", "ARRAY", "BACKTRACKING_PATTERN"),
    ("number-of-islands", "Number of Islands", "MEDIUM", "GRAPH", "DFS"),
    ("clone-graph", "Clone Graph", "MEDIUM", "GRAPH", "DFS"),
    ("pacific-atlantic-water-flow", "Pacific Atlantic Water Flow", "MEDIUM", "GRAPH", "DFS"),
    ("course-schedule", "Course Schedule", "MEDIUM", "GRAPH", "TOPOLOGICAL_SORT"),
    ("course-schedule-ii", "Course Schedule II", "MEDIUM", "GRAPH", "TOPOLOGICAL_SORT"),
    ("graph-valid-tree", "Graph Valid Tree", "MEDIUM", "GRAPH", "UNION_FIND"),
    ("number-of-connected-components-in-an-undirected-graph", "Number of Connected Components", "MEDIUM", "GRAPH", "UNION_FIND"),
    ("redundant-connection", "Redundant Connection", "MEDIUM", "GRAPH", "UNION_FIND"),
    ("word-ladder", "Word Ladder", "HARD", "GRAPH", "BFS"),
    ("network-delay-time", "Network Delay Time", "MEDIUM", "GRAPH", "SHORTEST_PATH"),
    ("cheapest-flights-within-k-stops", "Cheapest Flights Within K Stops", "MEDIUM", "GRAPH", "SHORTEST_PATH"),
    ("implement-queue-using-stacks", "Implement Queue using Stacks", "EASY", "STACK", "STACK"),
    ("kth-largest-element-in-an-array", "Kth Largest Element in an Array", "MEDIUM", "ARRAY", "TOP_K"),
    ("find-median-from-data-stream", "Find Median from Data Stream", "HARD", "HEAP", "TOP_K"),
    ("task-scheduler", "Task Scheduler", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("design-twitter", "Design Twitter", "MEDIUM", "HEAP", "TOP_K"),
    ("longest-consecutive-sequence", "Longest Consecutive Sequence", "MEDIUM", "ARRAY", "HASHING"),
    ("encode-and-decode-strings", "Encode and Decode Strings", "MEDIUM", "STRING", "HASHING"),
    ("longest-substring-without-repeating-characters", "Longest Substring Without Repeating Characters", "MEDIUM", "STRING", "SLIDING_WINDOW"),
    ("longest-repeating-character-replacement", "Longest Repeating Character Replacement", "MEDIUM", "STRING", "SLIDING_WINDOW"),
    ("minimum-window-substring", "Minimum Window Substring", "HARD", "STRING", "SLIDING_WINDOW"),
    ("valid-palindrome", "Valid Palindrome", "EASY", "STRING", "TWO_POINTER"),
    ("palindromic-substrings", "Palindromic Substrings", "MEDIUM", "STRING", "TWO_POINTER"),
    ("palindrome-partitioning", "Palindrome Partitioning", "MEDIUM", "STRING", "BACKTRACKING_PATTERN"),
    ("subsets", "Subsets", "MEDIUM", "ARRAY", "BACKTRACKING_PATTERN"),
    ("combination-sum-ii", "Combination Sum II", "MEDIUM", "ARRAY", "BACKTRACKING_PATTERN"),
    ("permutations", "Permutations", "MEDIUM", "ARRAY", "BACKTRACKING_PATTERN"),
    ("subsets-ii", "Subsets II", "MEDIUM", "ARRAY", "BACKTRACKING_PATTERN"),
    ("letter-combinations-of-a-phone-number", "Letter Combinations of a Phone Number", "MEDIUM", "STRING", "BACKTRACKING_PATTERN"),
    ("n-queens", "N-Queens", "HARD", "ARRAY", "BACKTRACKING_PATTERN"),
    ("search-a-2d-matrix", "Search a 2D Matrix", "MEDIUM", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("koko-eating-bananas", "Koko Eating Bananas", "MEDIUM", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("time-based-key-value-store", "Time Based Key-Value Store", "MEDIUM", "STRING", "BINARY_SEARCH_PATTERN"),
    ("median-of-two-sorted-arrays", "Median of Two Sorted Arrays", "HARD", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("reverse-nodes-in-k-group", "Reverse Nodes in k-Group", "HARD", "LINKED_LIST", "TWO_POINTER"),
    ("trapping-rain-water", "Trapping Rain Water", "HARD", "ARRAY", "TWO_POINTER"),
    ("sliding-window-maximum", "Sliding Window Maximum", "HARD", "ARRAY", "SLIDING_WINDOW"),
    ("alien-dictionary", "Alien Dictionary", "HARD", "GRAPH", "TOPOLOGICAL_SORT"),
    ("reconstruct-itinerary", "Reconstruct Itinerary", "HARD", "GRAPH", "DFS"),
    ("min-cost-to-connect-all-points", "Min Cost to Connect All Points", "MEDIUM", "GRAPH", "UNION_FIND"),
    ("swim-in-rising-water", "Swim in Rising Water", "HARD", "GRAPH", "UNION_FIND"),
    ("rotting-oranges", "Rotting Oranges", "MEDIUM", "GRAPH", "BFS"),
    ("walls-and-gates", "Walls and Gates", "MEDIUM", "GRAPH", "BFS"),
    ("surrounded-regions", "Surrounded Regions", "MEDIUM", "GRAPH", "DFS"),
    ("accounts-merge", "Accounts Merge", "MEDIUM", "GRAPH", "UNION_FIND"),
    ("number-of-provinces", "Number of Provinces", "MEDIUM", "GRAPH", "UNION_FIND"),
    ("hand-of-straights", "Hand of Straights", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("partition-labels", "Partition Labels", "MEDIUM", "STRING", "GREEDY_PATTERN"),
    ("gas-station", "Gas Station", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("partition-equal-subset-sum", "Partition Equal Subset Sum", "MEDIUM", "DP", "KNAPSACK"),
    ("target-sum", "Target Sum", "MEDIUM", "DP", "KNAPSACK"),
    ("coin-change-ii", "Coin Change II", "MEDIUM", "DP", "KNAPSACK"),
    ("longest-palindromic-substring", "Longest Palindromic Substring", "MEDIUM", "STRING", "DP"),
    ("palindrome-partitioning-ii", "Palindrome Partitioning II", "HARD", "STRING", "DP"),
    ("burst-balloons", "Burst Balloons", "HARD", "DP", "DP"),
    ("edit-distance", "Edit Distance", "MEDIUM", "STRING", "DP"),
    ("interleaving-string", "Interleaving String", "MEDIUM", "STRING", "DP"),
    ("maximal-rectangle", "Maximal Rectangle", "HARD", "ARRAY", "MONOTONIC_STACK"),
    ("lru-cache", "LRU Cache", "MEDIUM", "LINKED_LIST", "HASHING"),
    ("add-two-numbers", "Add Two Numbers", "MEDIUM", "LINKED_LIST", "TWO_POINTER"),
    ("copy-list-with-random-pointer", "Copy List with Random Pointer", "MEDIUM", "LINKED_LIST", "HASHING"),
    ("find-the-duplicate-number", "Find the Duplicate Number", "MEDIUM", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("top-k-frequent-elements", "Top K Frequent Elements", "MEDIUM", "ARRAY", "TOP_K"),
    ("sort-colors", "Sort Colors", "MEDIUM", "ARRAY", "SORTING"),
    ("subarray-sum-equals-k", "Subarray Sum Equals K", "MEDIUM", "ARRAY", "PREFIX_SUM"),
    ("range-sum-query-immutable", "Range Sum Query - Immutable", "EASY", "ARRAY", "PREFIX_SUM"),
    ("binary-search", "Binary Search", "EASY", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("search-insert-position", "Search Insert Position", "EASY", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("sqrtx", "Sqrt(x)", "EASY", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("powx-n", "Pow(x, n)", "MEDIUM", "ARRAY", "RECURSION"),
    ("generate-parentheses", "Generate Parentheses", "MEDIUM", "STRING", "BACKTRACKING_PATTERN"),
    ("combination-sum-iv", "Combination Sum IV", "MEDIUM", "DP", "DP"),
    ("perfect-squares", "Perfect Squares", "MEDIUM", "DP", "DP"),
    ("word-break-ii", "Word Break II", "HARD", "STRING", "BACKTRACKING_PATTERN"),
    ("minimum-height-trees", "Minimum Height Trees", "MEDIUM", "GRAPH", "BFS"),
    ("reorganize-string", "Reorganize String", "MEDIUM", "STRING", "GREEDY_PATTERN"),
    ("ipo", "IPO", "HARD", "HEAP", "GREEDY_PATTERN"),
    ("find-all-anagrams-in-a-string", "Find All Anagrams in a String", "MEDIUM", "STRING", "SLIDING_WINDOW"),
    ("permutation-in-string", "Permutation in String", "MEDIUM", "STRING", "SLIDING_WINDOW"),
    ("fruit-into-baskets", "Fruit Into Baskets", "MEDIUM", "ARRAY", "SLIDING_WINDOW"),
    ("next-permutation", "Next Permutation", "MEDIUM", "ARRAY", "TWO_POINTER"),
    ("rotate-array", "Rotate Array", "MEDIUM", "ARRAY", "TWO_POINTER"),
    ("move-zeroes", "Move Zeroes", "EASY", "ARRAY", "TWO_POINTER"),
    ("remove-duplicates-from-sorted-array", "Remove Duplicates from Sorted Array", "EASY", "ARRAY", "TWO_POINTER"),
    ("remove-element", "Remove Element", "EASY", "ARRAY", "TWO_POINTER"),
    ("happy-number", "Happy Number", "EASY", "ARRAY", "HASHING"),
    ("isomorphic-strings", "Isomorphic Strings", "EASY", "STRING", "HASHING"),
    ("ransom-note", "Ransom Note", "EASY", "STRING", "HASHING"),
    ("majority-element", "Majority Element", "EASY", "ARRAY", "HASHING"),
    ("intersection-of-two-linked-lists", "Intersection of Two Linked Lists", "EASY", "LINKED_LIST", "TWO_POINTER"),
    ("path-sum", "Path Sum", "EASY", "TREE", "DFS"),
    ("path-sum-ii", "Path Sum II", "MEDIUM", "TREE", "DFS"),
    ("symmetric-tree", "Symmetric Tree", "EASY", "TREE", "DFS"),
    ("count-good-nodes-in-binary-tree", "Count Good Nodes in Binary Tree", "MEDIUM", "TREE", "DFS"),
    ("binary-tree-right-side-view", "Binary Tree Right Side View", "MEDIUM", "TREE", "BFS"),
    ("average-of-levels-in-binary-tree", "Average of Levels in Binary Tree", "EASY", "TREE", "BFS"),
    ("populating-next-right-pointers-in-each-node", "Populating Next Right Pointers in Each Node", "MEDIUM", "TREE", "BFS"),
    ("flatten-binary-tree-to-linked-list", "Flatten Binary Tree to Linked List", "MEDIUM", "TREE", "DFS"),
    ("delete-node-in-a-bst", "Delete Node in a BST", "MEDIUM", "TREE", "DFS"),
    ("range-sum-of-bst", "Range Sum of BST", "EASY", "TREE", "DFS"),
    ("insert-into-a-binary-search-tree", "Insert into a Binary Search Tree", "MEDIUM", "TREE", "DFS"),
    ("lowest-common-ancestor-of-a-binary-tree", "Lowest Common Ancestor of a Binary Tree", "MEDIUM", "TREE", "DFS"),
    ("all-nodes-distance-k-in-binary-tree", "All Nodes Distance K in Binary Tree", "MEDIUM", "TREE", "DFS"),
    ("count-complete-tree-nodes", "Count Complete Tree Nodes", "MEDIUM", "TREE", "DFS"),
    ("kth-smallest-element-in-a-sorted-matrix", "Kth Smallest Element in a Sorted Matrix", "MEDIUM", "HEAP", "BINARY_SEARCH_PATTERN"),
    ("find-k-pairs-with-smallest-sums", "Find K Pairs with Smallest Sums", "MEDIUM", "HEAP", "TOP_K"),
    ("ugly-number-ii", "Ugly Number II", "MEDIUM", "HEAP", "TOP_K"),
    ("reorganize-string", "Reorganize String", "MEDIUM", "STRING", "GREEDY_PATTERN"),
]

# Deduplicate blind 75
seen = set()
BLIND_75_UNIQUE = []
for p in BLIND_75:
    if p[0] not in seen:
        seen.add(p[0])
        BLIND_75_UNIQUE.append(p)
BLIND_75 = BLIND_75_UNIQUE[:75]

NEETCODE_EXTRA = [
    ("valid-sudoku", "Valid Sudoku", "MEDIUM", "ARRAY", "HASHING"),
    ("sudoku-solver", "Sudoku Solver", "HARD", "ARRAY", "BACKTRACKING_PATTERN"),
    ("design-hashmap", "Design HashMap", "EASY", "ARRAY", "HASHING"),
    ("design-hashset", "Design HashSet", "EASY", "ARRAY", "HASHING"),
    ("design-linked-list", "Design Linked List", "MEDIUM", "LINKED_LIST", "TWO_POINTER"),
    ("design-circular-queue", "Design Circular Queue", "MEDIUM", "QUEUE", "QUEUE"),
    ("design-browser-history", "Design Browser History", "MEDIUM", "LINKED_LIST", "TWO_POINTER"),
    ("design-a-stack-with-increment-operation", "Design a Stack With Increment Operation", "MEDIUM", "STACK", "STACK"),
    ("design-hit-counter", "Design Hit Counter", "MEDIUM", "QUEUE", "QUEUE"),
    ("design-underground-system", "Design Underground System", "MEDIUM", "HASHING", "HASHING"),
    ("design-parking-system", "Design Parking System", "EASY", "ARRAY", "HASHING"),
    ("design-authentication-manager", "Design Authentication Manager", "MEDIUM", "HASHING", "HASHING"),
    ("design-a-food-rating-system", "Design a Food Rating System", "MEDIUM", "HEAP", "TOP_K"),
    ("design-a-number-container-system", "Design a Number Container System", "MEDIUM", "HASHING", "HASHING"),
    ("design-most-recently-used-queue", "Design Most Recently Used Queue", "MEDIUM", "QUEUE", "QUEUE"),
    ("design-a-txt-file-sharing-system", "Design a Text File Sharing System", "HARD", "HASHING", "HASHING"),
    ("design-in-memory-file-system", "Design In-Memory File System", "HARD", "STRING", "DFS"),
    ("design-search-autocomplete-system", "Design Search Autocomplete System", "HARD", "STRING", "TOP_K"),
    ("design-a-leaderboard", "Design a Leaderboard", "MEDIUM", "HASHING", "SORTING"),
    ("design-a-stack-with-increment-operation", "Design Stack With Increment", "MEDIUM", "STACK", "STACK"),
]

def write_platform_csv(filename, problems):
    path = os.path.join(OUT, filename)
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["slug", "title", "difficulty", "topic", "pattern"])
        for p in problems:
            w.writerow(p)
    print(f"Wrote {len(problems)} rows to {filename}")

def gen_extra_lc(n, existing_slugs):
    extras = []
    i = 0
    while len(extras) < n:
        slug = f"lc-practice-{i:04d}"
        if slug in existing_slugs:
            i += 1
            continue
        existing_slugs.add(slug)
        topic = random.choice(TOPICS)
        pattern = random.choice(PATTERNS)
        diff = random.choice(DIFFS)
        title = f"Practice Problem {i}"
        extras.append((slug, title, diff, topic, pattern))
        i += 1
    return extras

# Build LeetCode dataset
lc_slugs = set()
lc_problems = list(BLIND_75)
for p in lc_problems:
    lc_slugs.add(p[0])

for p in NEETCODE_EXTRA:
    if p[0] not in lc_slugs:
        lc_problems.append(p)
        lc_slugs.add(p[0])

# Pad to ~200+ for NeetCode coverage
lc_problems.extend(gen_extra_lc(max(0, 200 - len(lc_problems)), lc_slugs))
write_platform_csv("leetcode.csv", lc_problems)

# Codeforces problems
cf_problems = [
    ("4A", "Watermelon", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("1A", "Theatre Square", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("158A", "Next Round", "EASY", "ARRAY", "TWO_POINTER"),
    ("50A", "Domino piling", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("231A", "Team", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("282A", "Bit++", "EASY", "STRING", "HASHING"),
    ("339A", "Helpful Maths", "EASY", "STRING", "SORTING"),
    ("479A", "Counterculture", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("977A", "Wrong Subtraction", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("546A", "Soldier and Bananas", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("1328A", "Carrot Strings", "EASY", "STRING", "GREEDY_PATTERN"),
    ("1368A", "C+=", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("1374A", "Required Remainder", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("1426A", "Fast Food Restaurant", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("1520A", "Do Not Be Distracted!", "EASY", "STRING", "HASHING"),
    ("1535A", "Fair Playoff", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("1547A", "Average Height", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("1551A", "Polycarp and Coins", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("160A", "Twins", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("200B", "Drinks", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("263A", "Beautiful Matrix", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("271A", "Beautiful Year", "EASY", "ARRAY", "HASHING"),
    ("486A", "Calculating Function", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("617A", "Elephant", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("734A", "Anton and Danik", "EASY", "STRING", "HASHING"),
    ("791A", "Bear and Big Brother", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("996A", "Hit the Lottery", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("112A", "Petya and Strings", "EASY", "STRING", "HASHING"),
    ("118A", "String Task", "EASY", "STRING", "HASHING"),
    ("122A", "Lucky Division", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("133A", "HQ9+", "EASY", "STRING", "HASHING"),
    ("160A", "Twins", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("200B", "Drinks", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("231A", "Team", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("282A", "Bit++", "EASY", "STRING", "HASHING"),
    ("339A", "Helpful Maths", "EASY", "STRING", "SORTING"),
    ("432A", "I Wanna Be the Guy", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("467A", "George and Accommodation", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("492A", "Vanya and Cubes", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("499B", "Lunch Rush", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("520A", "Pangram", "EASY", "STRING", "HASHING"),
    ("546A", "Soldier and Bananas", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("580A", "Kefa and First Steps", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("617A", "Elephant", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("677A", "Vanya and Fence", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("734A", "Anton and Danik", "EASY", "STRING", "HASHING"),
    ("791A", "Bear and Big Brother", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("977A", "Wrong Subtraction", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("996A", "Hit the Lottery", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("1028A", "Letters", "EASY", "STRING", "HASHING"),
]
seen_cf = set()
cf_unique = []
for p in cf_problems:
    if p[0] not in seen_cf:
        seen_cf.add(p[0])
        cf_unique.append(p)
cf_problems = cf_unique
cf_problems.extend(gen_extra_lc(30, seen_cf))  # reuse slug gen with cf prefix
cf_problems = [(f"cf-{p[0]}" if not p[0].startswith("cf-") else p[0], p[1].replace("Practice", "CF Practice"), p[2], p[3], p[4]) 
               if p[0].startswith("lc-") else p for p in cf_problems[:50]]
write_platform_csv("codeforces.csv", cf_problems)

# GFG problems
gfg_base = [
    ("reverse-an-array", "Reverse an array", "EASY", "ARRAY", "TWO_POINTER"),
    ("find-the-maximum-and-minimum-element-in-an-array", "Find Max and Min in Array", "EASY", "ARRAY", "TWO_POINTER"),
    ("kth-largest-element-in-an-array", "Kth largest element in an array", "MEDIUM", "ARRAY", "TOP_K"),
    ("sort-an-array-of-0s-1s-and-2s", "Sort 0s 1s 2s", "MEDIUM", "ARRAY", "TWO_POINTER"),
    ("find-the-union-and-intersection-of-two-sorted-arrays", "Union and Intersection", "EASY", "ARRAY", "TWO_POINTER"),
    ("cyclically-rotate-an-array-by-one", "Rotate array by one", "EASY", "ARRAY", "TWO_POINTER"),
    ("find-the-missing-number", "Find the missing number", "EASY", "ARRAY", "HASHING"),
    ("search-an-element-in-a-sorted-and-rotated-array", "Search in rotated sorted array", "MEDIUM", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("find-pair-with-given-sum", "Find pair with given sum", "EASY", "ARRAY", "HASHING"),
    ("maximum-subarray-sum", "Maximum subarray sum", "MEDIUM", "ARRAY", "SLIDING_WINDOW"),
    ("merge-two-sorted-arrays", "Merge two sorted arrays", "EASY", "ARRAY", "TWO_POINTER"),
    ("kadanes-algorithm", "Kadane's Algorithm", "MEDIUM", "ARRAY", "SLIDING_WINDOW"),
    ("stock-buy-and-sell", "Stock buy and sell", "EASY", "ARRAY", "SLIDING_WINDOW"),
    ("find-the-duplicate-number", "Find the duplicate number", "MEDIUM", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("merge-intervals", "Merge Intervals", "MEDIUM", "ARRAY", "INTERVALS"),
    ("n-meetings-in-one-room", "N meetings in one room", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("minimum-platforms", "Minimum Platforms", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("job-sequencing-problem", "Job Sequencing Problem", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("activity-selection-problem", "Activity Selection Problem", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("fractional-knapsack-problem", "Fractional Knapsack", "MEDIUM", "DP", "KNAPSACK"),
    ("0-1-knapsack-problem", "0-1 Knapsack", "MEDIUM", "DP", "KNAPSACK"),
    ("longest-common-subsequence", "Longest Common Subsequence", "MEDIUM", "STRING", "DP"),
    ("edit-distance-dp-5", "Edit Distance", "MEDIUM", "STRING", "DP"),
    ("subset-sum-problem-dp-25", "Subset Sum Problem", "MEDIUM", "DP", "KNAPSACK"),
    ("coin-change-dp-7", "Coin Change", "MEDIUM", "DP", "KNAPSACK"),
    ("matrix-chain-multiplication-dp-8", "Matrix Chain Multiplication", "HARD", "DP", "DP"),
    ("longest-palindromic-subsequence-dp-13", "Longest Palindromic Subsequence", "MEDIUM", "STRING", "DP"),
    ("implement-stack-using-array", "Implement Stack using Array", "EASY", "STACK", "STACK"),
    ("implement-queue-using-array", "Implement Queue using Array", "EASY", "QUEUE", "QUEUE"),
    ("reverse-a-stack", "Reverse a Stack", "MEDIUM", "STACK", "STACK"),
    ("next-greater-element", "Next Greater Element", "MEDIUM", "STACK", "MONOTONIC_STACK"),
    ("largest-rectangle-in-a-histogram", "Largest Rectangle in Histogram", "HARD", "STACK", "MONOTONIC_STACK"),
    ("level-order-traversal", "Level Order Traversal", "MEDIUM", "TREE", "BFS"),
    ("inorder-traversal", "Inorder Traversal", "EASY", "TREE", "DFS"),
    ("preorder-traversal", "Preorder Traversal", "EASY", "TREE", "DFS"),
    ("postorder-traversal", "Postorder Traversal", "EASY", "TREE", "DFS"),
    ("height-of-binary-tree", "Height of Binary Tree", "EASY", "TREE", "DFS"),
    ("diameter-of-binary-tree", "Diameter of Binary Tree", "EASY", "TREE", "DFS"),
    ("lowest-common-ancestor-in-a-binary-tree", "LCA in Binary Tree", "MEDIUM", "TREE", "DFS"),
    ("check-if-binary-tree-is-balanced", "Check if Tree is Balanced", "EASY", "TREE", "DFS"),
    ("detect-cycle-in-an-undirected-graph", "Detect Cycle Undirected Graph", "MEDIUM", "GRAPH", "DFS"),
    ("detect-cycle-in-a-directed-graph", "Detect Cycle Directed Graph", "MEDIUM", "GRAPH", "DFS"),
    ("topological-sort", "Topological Sort", "MEDIUM", "GRAPH", "TOPOLOGICAL_SORT"),
    ("shortest-path-in-unweighted-graph", "Shortest Path Unweighted", "MEDIUM", "GRAPH", "BFS"),
    ("dijkstra-algorithm", "Dijkstra Algorithm", "MEDIUM", "GRAPH", "SHORTEST_PATH"),
    ("bellman-ford-algorithm", "Bellman Ford Algorithm", "MEDIUM", "GRAPH", "SHORTEST_PATH"),
    ("floyd-warshall", "Floyd Warshall", "MEDIUM", "GRAPH", "SHORTEST_PATH"),
    ("kruskal-minimum-spanning-tree-algorithm", "Kruskal MST", "HARD", "GRAPH", "UNION_FIND"),
    ("prims-minimum-spanning-tree-algorithm", "Prims MST", "HARD", "GRAPH", "GREEDY_PATTERN"),
    ("disjoint-set-union-find", "Disjoint Set Union Find", "MEDIUM", "GRAPH", "UNION_FIND"),
]
gfg_slugs = {p[0] for p in gfg_base}
gfg_base.extend(gen_extra_lc(80, gfg_slugs))
write_platform_csv("gfg.csv", gfg_base)

# HackerRank problems
hr_problems = [
    ("arrays-ds", "Arrays DS", "EASY", "ARRAY", "TWO_POINTER"),
    ("2d-array-ds", "2D Array DS", "EASY", "ARRAY", "TWO_POINTER"),
    ("array-left-rotation", "Left Rotation", "EASY", "ARRAY", "TWO_POINTER"),
    ("new-year-chaos", "New Year Chaos", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("minimum-swaps-2", "Minimum Swaps 2", "MEDIUM", "ARRAY", "HASHING"),
    ("array-manipulation", "Array Manipulation", "HARD", "ARRAY", "PREFIX_SUM"),
    ("cracking-the-coding-interview-arrays-left-rotation", "Left Rotation CTCI", "EASY", "ARRAY", "TWO_POINTER"),
    ("cracking-the-coding-interview-arrays-array-left-rotation", "Array Left Rotation", "EASY", "ARRAY", "TWO_POINTER"),
    ("hash-tables-ransom-note", "Ransom Note", "EASY", "STRING", "HASHING"),
    ("hash-tables-two-sum", "Two Sum HR", "EASY", "ARRAY", "HASHING"),
    ("hash-tables-check-complete", "Check Complete", "EASY", "STRING", "HASHING"),
    ("hash-tables-sherlock-and-anagrams", "Sherlock and Anagrams", "MEDIUM", "STRING", "HASHING"),
    ("hash-tables-frequency-queries", "Frequency Queries", "MEDIUM", "ARRAY", "HASHING"),
    ("trees-insert-a-node-into-a-binary-search-tree", "Insert BST Node", "EASY", "TREE", "DFS"),
    ("trees-height-of-a-binary-tree", "Height of Binary Tree HR", "EASY", "TREE", "DFS"),
    ("trees-level-order-traversal", "Level Order Traversal HR", "EASY", "TREE", "BFS"),
    ("trees-huffman-decoding", "Huffman Decoding", "MEDIUM", "TREE", "GREEDY_PATTERN"),
    ("trees-swap-nodes", "Swap Nodes", "MEDIUM", "TREE", "BFS"),
    ("trees-is-this-a-binary-search-tree", "Is BST", "MEDIUM", "TREE", "DFS"),
    ("trees-kittys-calculations-on-a-tree", "Kitty Calculations on Tree", "HARD", "TREE", "DFS"),
    ("stacks-balanced-brackets", "Balanced Brackets", "MEDIUM", "STRING", "STACK"),
    ("stacks-next-greater-element", "Next Greater Element HR", "MEDIUM", "STACK", "MONOTONIC_STACK"),
    ("stacks-largest-rectangle", "Largest Rectangle HR", "HARD", "STACK", "MONOTONIC_STACK"),
    ("stacks-and-queues-queue-using-two-stacks", "Queue using Two Stacks", "MEDIUM", "STACK", "STACK"),
    ("stacks-and-queues-and-in-one", "Stack and Queue in One", "MEDIUM", "STACK", "STACK"),
    ("queues-ctci", "Queue CTCI", "EASY", "QUEUE", "QUEUE"),
    ("heap-find-the-running-median", "Find the Running Median", "HARD", "HEAP", "TOP_K"),
    ("heap-qheap1", "QHEAP1", "MEDIUM", "HEAP", "TOP_K"),
    ("heap-sort", "Heap Sort", "MEDIUM", "HEAP", "SORTING"),
    ("greedy-algorithms-jim-and-the-orders", "Jim and the Orders", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("greedy-algorithms-luck-balance", "Luck Balance", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("greedy-algorithms-maximum-perimeter-triangle", "Maximum Perimeter Triangle", "EASY", "ARRAY", "GREEDY_PATTERN"),
    ("greedy-algorithms-sherlock-and-the-beast", "Sherlock and the Beast", "MEDIUM", "STRING", "GREEDY_PATTERN"),
    ("greedy-algorithms-priyanka-and-the-twisted-shopping", "Priyanka Shopping", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("dynamic-programming-coin-change", "Coin Change HR", "MEDIUM", "DP", "KNAPSACK"),
    ("dynamic-programming-knapsack", "Knapsack HR", "MEDIUM", "DP", "KNAPSACK"),
    ("dynamic-programming-longest-common-subsequence", "LCS HR", "MEDIUM", "STRING", "DP"),
    ("dynamic-programming-edit-distance", "Edit Distance HR", "MEDIUM", "STRING", "DP"),
    ("dynamic-programming-maximum-subarray", "Max Subarray HR", "EASY", "ARRAY", "SLIDING_WINDOW"),
    ("dynamic-programming-sherlock-and-cost", "Sherlock and Cost", "MEDIUM", "DP", "DP"),
    ("graph-theory-journey-to-the-moon", "Journey to the Moon", "MEDIUM", "GRAPH", "UNION_FIND"),
    ("graph-theory-roads-and-libraries", "Roads and Libraries", "MEDIUM", "GRAPH", "UNION_FIND"),
    ("graph-theory-synchronous-shopping", "Synchronous Shopping", "MEDIUM", "GRAPH", "BFS"),
    ("graph-theory-kruskalmstrals-algorithm", "Kruskal MST HR", "MEDIUM", "GRAPH", "UNION_FIND"),
    ("graph-theory-prims-minimum-spanning-tree", "Prims MST HR", "MEDIUM", "GRAPH", "GREEDY_PATTERN"),
    ("graph-theory-shortest-path", "Shortest Path HR", "MEDIUM", "GRAPH", "SHORTEST_PATH"),
    ("graph-theory-bfsshortreach", "BFS Short Reach", "MEDIUM", "GRAPH", "BFS"),
    ("graph-theory-dijkstrashortreach", "Dijkstra Short Reach", "HARD", "GRAPH", "SHORTEST_PATH"),
    ("search-binary-search", "Binary Search HR", "EASY", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("search-ice-cream-parlor", "Ice Cream Parlor", "EASY", "ARRAY", "HASHING"),
    ("search-triple-sum", "Triple Sum HR", "MEDIUM", "ARRAY", "TWO_POINTER"),
    ("search-minimum-loss", "Minimum Loss", "MEDIUM", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("search-pairs", "Pairs HR", "MEDIUM", "ARRAY", "TWO_POINTER"),
    ("sorting-counting-sort-1", "Counting Sort 1", "EASY", "ARRAY", "SORTING"),
    ("sorting-counting-sort-2", "Counting Sort 2", "MEDIUM", "ARRAY", "SORTING"),
    ("sorting-insertion-sort", "Insertion Sort", "EASY", "ARRAY", "SORTING"),
    ("sorting-quick-sort", "Quick Sort", "MEDIUM", "ARRAY", "SORTING"),
    ("sorting-almost-sorted", "Almost Sorted", "MEDIUM", "ARRAY", "SORTING"),
    ("strings-sherlock-and-valid-string", "Sherlock Valid String", "MEDIUM", "STRING", "GREEDY_PATTERN"),
    ("strings-ashton-and-string", "Ashton and String", "HARD", "STRING", "HASHING"),
    ("strings-common-child", "Common Child", "MEDIUM", "STRING", "DP"),
    ("strings-morgan-and-a-string", "Morgan and a String", "HARD", "STRING", "GREEDY_PATTERN"),
    ("interview-preparation-kit-arrays-two-sum", "IPK Two Sum", "EASY", "ARRAY", "HASHING"),
    ("interview-preparation-kit-arrays-array-manipulation", "IPK Array Manipulation", "HARD", "ARRAY", "PREFIX_SUM"),
    ("interview-preparation-kit-arrays-new-year-chaos", "IPK New Year Chaos", "MEDIUM", "ARRAY", "GREEDY_PATTERN"),
    ("interview-preparation-kit-dictionaries-hash-tables-ransom-note", "IPK Ransom Note", "EASY", "STRING", "HASHING"),
    ("interview-preparation-kit-dictionaries-hash-tables-two-sum", "IPK Hash Two Sum", "EASY", "ARRAY", "HASHING"),
    ("interview-preparation-kit-dictionaries-hash-tables-sherlock-and-anagrams", "IPK Anagrams", "MEDIUM", "STRING", "HASHING"),
    ("interview-preparation-kit-dictionaries-hash-tables-frequency-queries", "IPK Freq Queries", "MEDIUM", "ARRAY", "HASHING"),
    ("interview-preparation-kit-string-manipulation-alternating-characters", "IPK Alternating Chars", "EASY", "STRING", "SLIDING_WINDOW"),
    ("interview-preparation-kit-string-manipulation-special-palindrome-again", "IPK Special Palindrome", "MEDIUM", "STRING", "HASHING"),
    ("interview-preparation-kit-string-manipulation-sherlock-and-the-valid-string", "IPK Valid String", "MEDIUM", "STRING", "GREEDY_PATTERN"),
    ("interview-preparation-kit-string-manipulation-common-child", "IPK Common Child", "MEDIUM", "STRING", "DP"),
    ("interview-preparation-kit-string-manipulation-morgan-and-a-string", "IPK Morgan String", "HARD", "STRING", "GREEDY_PATTERN"),
    ("interview-preparation-kit-sorting-bubble-sort", "IPK Bubble Sort", "EASY", "ARRAY", "SORTING"),
    ("interview-preparation-kit-sorting-counting-sort", "IPK Counting Sort", "MEDIUM", "ARRAY", "SORTING"),
    ("interview-preparation-kit-sorting-insertion-sort", "IPK Insertion Sort", "EASY", "ARRAY", "SORTING"),
    ("interview-preparation-kit-sorting-quick-sort", "IPK Quick Sort", "MEDIUM", "ARRAY", "SORTING"),
    ("interview-preparation-kit-search-minimum-loss", "IPK Minimum Loss", "MEDIUM", "ARRAY", "BINARY_SEARCH_PATTERN"),
    ("interview-preparation-kit-search-triple-sum", "IPK Triple Sum", "MEDIUM", "ARRAY", "TWO_POINTER"),
    ("interview-preparation-kit-search-pairs", "IPK Pairs", "MEDIUM", "ARRAY", "TWO_POINTER"),
    ("interview-preparation-kit-stacks-balanced-brackets", "IPK Balanced Brackets", "MEDIUM", "STRING", "STACK"),
    ("interview-preparation-kit-stacks-queues-largest-rectangle", "IPK Largest Rectangle", "HARD", "STACK", "MONOTONIC_STACK"),
    ("interview-preparation-kit-trees-height-of-a-binary-tree", "IPK Tree Height", "EASY", "TREE", "DFS"),
    ("interview-preparation-kit-trees-swap-nodes", "IPK Swap Nodes", "MEDIUM", "TREE", "BFS"),
    ("interview-preparation-kit-trees-is-this-a-binary-search-tree", "IPK Is BST", "MEDIUM", "TREE", "DFS"),
    ("interview-preparation-kit-dynamic-programming-coin-change", "IPK Coin Change", "MEDIUM", "DP", "KNAPSACK"),
    ("interview-preparation-kit-dynamic-programming-knapsack", "IPK Knapsack", "MEDIUM", "DP", "KNAPSACK"),
    ("interview-preparation-kit-dynamic-programming-longest-common-subsequence", "IPK LCS", "MEDIUM", "STRING", "DP"),
    ("interview-preparation-kit-dynamic-programming-edit-distance", "IPK Edit Distance", "MEDIUM", "STRING", "DP"),
    ("interview-preparation-kit-dynamic-programming-maximum-subarray", "IPK Max Subarray", "EASY", "ARRAY", "SLIDING_WINDOW"),
    ("interview-preparation-kit-graph-theory-journey-to-the-moon", "IPK Journey Moon", "MEDIUM", "GRAPH", "UNION_FIND"),
    ("interview-preparation-kit-graph-theory-roads-and-libraries", "IPK Roads Libraries", "MEDIUM", "GRAPH", "UNION_FIND"),
    ("interview-preparation-kit-graph-theory-synchronous-shopping", "IPK Sync Shopping", "MEDIUM", "GRAPH", "BFS"),
    ("interview-preparation-kit-graph-theory-shortest-path", "IPK Shortest Path", "MEDIUM", "GRAPH", "SHORTEST_PATH"),
    ("interview-preparation-kit-graph-theory-bfsshortreach", "IPK BFS Reach", "MEDIUM", "GRAPH", "BFS"),
    ("interview-preparation-kit-graph-theory-dijkstrashortreach", "IPK Dijkstra", "HARD", "GRAPH", "SHORTEST_PATH"),
]
hr_slugs = {p[0] for p in hr_problems}
hr_problems.extend(gen_extra_lc(20, hr_slugs))
write_platform_csv("hackerrank.csv", hr_problems)

def write_sheet(filename, refs, target_count):
    """refs is list of (platform, slug) tuples; pad with generated refs if needed."""
    path = os.path.join(OUT, filename)
    padded = list(refs)
    i = 0
    while len(padded) < target_count:
        slug = lc_problems[i % len(lc_problems)][0]
        padded.append(("LEETCODE", slug))
        i += 1
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["platform", "slug"])
        for platform, slug in padded[:target_count]:
            w.writerow([platform, slug])
    print(f"Wrote {min(len(padded), target_count)} rows to {filename}")

blind_refs = [("LEETCODE", p[0]) for p in BLIND_75]
neet_refs = blind_refs + [("LEETCODE", p[0]) for p in lc_problems if p[0] not in {r[1] for r in blind_refs}]

write_sheet("blind_75.csv", blind_refs, 75)
write_sheet("neetcode_150.csv", neet_refs, 150)

# CodeHelp DSA - mix platforms
codehelp_refs = []
for p in gfg_base[:140]:
    codehelp_refs.append(("GFG", p[0]))
for p in lc_problems[:100]:
    codehelp_refs.append(("LEETCODE", p[0]))
for p in hr_problems[:40]:
    codehelp_refs.append(("HACKERRANK", p[0]))
write_sheet("codehelp_dsa.csv", codehelp_refs, 280)

# Striver A2Z - comprehensive mix
striver_refs = []
for p in lc_problems:
    striver_refs.append(("LEETCODE", p[0]))
for p in gfg_base:
    striver_refs.append(("GFG", p[0]))
for p in cf_problems:
    striver_refs.append(("CODEFORCES", p[0]))
for p in hr_problems:
    striver_refs.append(("HACKERRANK", p[0]))
write_sheet("striver_a2z.csv", striver_refs, 455)

print("Dataset generation complete.")
