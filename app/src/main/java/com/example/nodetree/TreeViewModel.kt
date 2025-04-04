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
            val root = if (treeJson == null) {
                TreeNode(TreeNode.generateNodeId("Root"), "Root")
            } else {
                Gson().fromJson(treeJson, TreeNode::class.java)
            }
            _treeState.update { it.copy(currentNode = root) }
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
        _treeState.update { state ->
            node.parent?.removeChild(node)
            saveTree()
            if (node == state.currentNode) {
                state.copy(currentNode = node.parent ?: state.currentNode.findRoot())
            } else {
                state.copy()
            }
        }
    }

    fun navigateUp() {
        _treeState.update { state ->
            state.currentNode.parent?.let { parent ->
                state.copy(currentNode = parent)
            } ?: state
        }
    }

    fun navigateTo(node: TreeNode) {
        _treeState.update { state ->
            state.copy(currentNode = node)
        }
    }

    fun setAddingNode(isAdding: Boolean) {
        _treeState.update { it.copy(isAddingNode = isAdding) }
    }
}