package cga.exercise.components.geometry

import cga.exercise.components.shader.ShaderProgram

/**
 * Extends Transformable such that the object can render Mesh objects transformed by Transformable
 */
class Renderable(val meshes: MutableList<Mesh> = mutableListOf()) : Transformable(parent = null), IRenderable {

    /**
     * Renders all meshes attached to this Renderable, applying the transformation matrix to
     * each of them
     */
    override fun render(shaderProgram: ShaderProgram) {
        shaderProgram.setUniform("model_matrix", getWorldModelMatrix(), false)
        for (m in meshes) {
            m.render(shaderProgram)
        }
    }
}