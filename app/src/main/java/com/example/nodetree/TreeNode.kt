package com.example.nodetree

import java.security.MessageDigest

data class TreeNode(
    val id: String,
    var name: String,
    @Transient var parent: TreeNode? = null,
    var children: MutableList<TreeNode> = mutableListOf()
) {
    companion object {
        fun generateNodeId(name: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(name.toByteArray(Charsets.UTF_8))
            val last20Bytes = hash.copyOfRange(hash.size - 20, hash.size)
            return last20Bytes.toHexString()
        }
    }

    fun addChild(child: TreeNode) {
        children.add(child)
        child.parent = this
    }

    fun removeChild(child: TreeNode) {
        children.remove(child)
        child.parent = null
    }

    fun findRoot(): TreeNode {
        var current = this
        while (current.parent != null) {
            current = current.parent!!
        }
        return current
    }
}

fun TreeNode.findNode(id: String): TreeNode? {
    if (this.id == id) return this
    for (child in children) {
        val found = child.findNode(id)
        if (found != null) return found
    }
    return null
}

fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}