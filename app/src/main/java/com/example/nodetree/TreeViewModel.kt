package com.example.nodetree

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.core.content.edit

class TreeViewModel(application: Application) : AndroidViewModel(application) {
    private val _treeState = MutableStateFlow(TreeState())
    val treeState: StateFlow<TreeState> = _treeState

    private val sharedPreferences: SharedPreferences by lazy {
        application.getSharedPreferences("node_tree_prefs", Context.MODE_PRIVATE)
    }

    init {
        loadTree()
    }

    private fun loadTree() {
        viewModelScope.launch {
            val treeJson = sharedPreferences.getString("tree", null)
            if (treeJson == null) {
                TreeNode(TreeNode.generateNodeId("Root"), "Root").also {
                    _treeState.value = TreeState(currentNode = it, rootNode = it)
                }
            } else {
                Gson().fromJson(treeJson, TreeNode::class.java).apply {
                    rebuildParentReferences()
                    _treeState.value = TreeState(currentNode = this, rootNode = this)
                }
            }
        }
    }

    private fun TreeNode.rebuildParentReferences() {
        children.forEach { child ->
            child.parent = this
            child.rebuildParentReferences()
        }
    }

    private fun saveTree() {
        viewModelScope.launch {
            sharedPreferences.edit() {
                putString(
                    "tree",
                    Gson().toJson(
                        _treeState.value.rootNode ?: _treeState.value.currentNode.findRoot()
                    )
                )
            }
        }
    }

    fun addNode(name: String) {
        _treeState.update { state ->
            val newNode = TreeNode(
                TreeNode.generateNodeId(name),
                name,
                state.currentNode
            )
            state.currentNode.addChild(newNode)
            saveTree()
            state.copy()
        }
    }

    fun removeNode(node: TreeNode) {
        _treeState.update { currentState ->
            val updatedCurrentNode = currentState.currentNode.deepCopy()

            updatedCurrentNode.findNode(node.id)?.let { nodeToRemove ->
                nodeToRemove.parent?.removeChild(nodeToRemove)
            }

            saveTree()

            if (node.id == currentState.currentNode.id) {
                currentState.copy(
                    currentNode = updatedCurrentNode.parent ?: updatedCurrentNode.findRoot(),
                    rootNode = updatedCurrentNode.findRoot()
                )
            } else {
                currentState.copy(
                    currentNode = updatedCurrentNode,
                    rootNode = updatedCurrentNode.findRoot()
                )
            }
        }
    }

    fun TreeNode.deepCopy(): TreeNode {
        val copy = TreeNode(this.id, this.name)
        copy.children = this.children.map { it.deepCopy() }.toMutableList()
        copy.children.forEach { it.parent = copy }
        return copy
    }

    fun TreeNode.findNode(id: String): TreeNode? {
        if (this.id == id) return this
        for (child in children) {
            val found = child.findNode(id)
            if (found != null) return found
        }
        return null
    }
    fun navigateUp() {
        _treeState.update { currentState ->
            currentState.currentNode.parent?.let { parent ->
                currentState.copy(currentNode = parent)
            } ?: currentState // Stay at root if no parent exists
        }
    }

    fun navigateTo(node: TreeNode) {
        _treeState.update { currentState ->
            val foundNode = currentState.rootNode?.findNode(node.id) ?: node
            currentState.copy(currentNode = foundNode)
        }
    }

    fun setAddingNode(isAdding: Boolean) {
        _treeState.update { it.copy(isAddingNode = isAdding) }
    }
}