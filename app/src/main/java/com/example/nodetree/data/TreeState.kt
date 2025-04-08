package com.example.nodetree.data

data class TreeState(
    val rootNode: TreeNode? = null,
    val currentNode: TreeNode = TreeNode("", "Root"),
    val isAddingNode: Boolean = false
)