package cga.exercise.components.texture

import cga.exercise.components.shader.ShaderProgram
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImage
import java.nio.ByteBuffer
/**
import cga.framework.GLError
import cga.framework.GLError.checkEx
import org.lwjgl.opengl.ARBTextureStorage.glTexStorage2D
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
 */

class CubeMap(imageDataArray: ArrayList<ByteBuffer>, width: Int, height: Int, genMipMaps: Boolean){
    private var texID: Int = -1

    /* used after invoke operation */
    init {
        try {
            loadCubeMap(imageDataArray, width, height, genMipMaps)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    companion object {
        //create texture from file
        //don't support compressed textures for now
        //instead stick to pngs
        operator fun invoke(paths: ArrayList<String>, genMipMaps: Boolean): CubeMap {
            val x = BufferUtils.createIntBuffer(1)
            val y = BufferUtils.createIntBuffer(1)
            val readChannels = BufferUtils.createIntBuffer(1)

            //flip y coordinate to make OpenGL happy
            STBImage.stbi_set_flip_vertically_on_load(true)

            /* Loading several Textures for CubeMap */
            val imageDataArray = arrayListOf<ByteBuffer>()
            for(path in paths){
                val imageData = STBImage.stbi_load(path, x, y, readChannels, 0)
                    ?: throw Exception("Image file \"" + path + "\" couldn't be read:\n" + STBImage.stbi_failure_reason())
                imageDataArray.add(imageData)
            }

            try {
                return CubeMap(imageDataArray, x.get(), y.get(), genMipMaps)
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                throw ex
            } finally {
                /* Freeing all loaded images*/
                for(image in imageDataArray) {
                    STBImage.stbi_image_free(image)
                }
            }
        }
    }

    fun loadCubeMap(imageDataArray: ArrayList<ByteBuffer>, width: Int, height: Int, genMipMaps: Boolean) {
        texID = glGenTextures()
        glBindTexture(GL_TEXTURE_CUBE_MAP, texID)

        var i = 0
        for(imageData in imageDataArray){

            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData)

            if(genMipMaps){
                glGenerateMipmap(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i)
            }
            i++
        }
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0)
    }

    fun setTexParams(){
        glBindTexture(GL_TEXTURE_CUBE_MAP, texID)

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glBindTexture(GL_TEXTURE_CUBE_MAP, 0)
    }

    fun bind(textureUnit: Int, shaderProgram : ShaderProgram) {
        glActiveTexture(GL_TEXTURE0 + textureUnit)
        glBindTexture(GL_TEXTURE_CUBE_MAP, texID)
        shaderProgram.setUniform("skybox", textureUnit)
    }

    fun unbind() {
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0)
    }

    fun cleanup() {
        unbind()
        if (texID != 0) {
            glDeleteTextures(texID)
            texID = 0
        }
    }
}