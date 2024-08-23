package cga.exercise.game

import cga.exercise.components.camera.Aspectratio.Companion.custom
import cga.exercise.components.camera.TronCamera
import cga.exercise.components.geometry.*
import cga.exercise.components.light.PointLight
import cga.exercise.components.light.SpotLight
import cga.exercise.components.shader.ShaderProgram
import cga.exercise.components.texture.CubeMap
import cga.exercise.components.texture.Texture2D
import cga.framework.GLError
import cga.framework.GameWindow
import cga.framework.ModelLoader.loadModel
import cga.framework.OBJLoader.loadOBJ
import org.joml.Math
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*
import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.*

class Scene(private val window: GameWindow) {

    /** Variable Declarations **/

    /** 1) shader programs **/
    private val staticShader: ShaderProgram
    private val skyShader: ShaderProgram
    /** 2) materials **/
    private val groundMaterial: Material
    private val skyboxTex: CubeMap
    /** 3) renderables **/
    private val ground: Renderable
    private val ball: Renderable
    private val finishLine: Renderable
    private val wall: Renderable
    private val wall2: Renderable
    private val wall3: Renderable
    private val wall4: Renderable
    private val wall5: Renderable
    private val wall6: Renderable
    private val wall7: Renderable
    private val wall8: Renderable
    private val wall9: Renderable
    private val wall10: Renderable
    private val skybox: Renderable

    /** 4) camera **/
    private val camera: TronCamera
    private var oldMouseX       = 0.0
    private var oldMouseY       = 0.0
    private var firstMouseMove  = true
    private val skyboxRotator = Transformable()
    /** 5) additional uniforms **/
    private val groundColor: Vector3f
    private val pointLightList: MutableList<PointLight>
    private val spotLightList: MutableList<SpotLight>

    /** Variable Definition/Initialisation **/
    init {
        /** shader programs **/
        staticShader = ShaderProgram("assets/shaders/tron_vert.glsl", "assets/shaders/tron_frag.glsl")
        skyShader = ShaderProgram("assets/shaders/skybox_vert.glsl", "assets/shaders/skybox_frag.glsl")
        // ############################################################################################# //

        /* BGM */
        val audioInputStream : AudioInputStream = AudioSystem.getAudioInputStream(File("assets/music/Coral_Chorus.wav"))
        val clip : Clip = AudioSystem.getClip()
        clip.open(audioInputStream)
        clip.loop(Clip.LOOP_CONTINUOUSLY)
        val gainControl : FloatControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        gainControl.value = -12.0f // decreased by 12dB => (1/4 of default volume)
        clip.start()

        /** materials **/
        val groundDiff      = Texture2D("assets/textures/ground_diff.png", true)
        groundDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        val groundSpecular  = Texture2D("assets/textures/ground_spec.png", true)
        groundSpecular.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        val groundEmit      = Texture2D("assets/textures/ground_emit.png", true)
        groundEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        groundMaterial      = Material(groundDiff, groundEmit, groundSpecular, 60f, Vector2f(64.0f, 64.0f))

        // ############################################################################################# //

        /** Vertex Buffer => per Vertex Attribute Declaration **/
        val byteStride = (3 + 2 + 3) * 4
        val atr1 = VertexAttribute(3, GL_FLOAT, byteStride, 0)     // position
        val atr2 = VertexAttribute(2, GL_FLOAT, byteStride, 3 * 4) // texture coordinate
        val atr3 = VertexAttribute(3, GL_FLOAT, byteStride, 5 * 4) // normal
        val vertexAttribs1 = arrayOf(atr1, atr2, atr3)

        /* CubeMap Textures */
        val cubeFaces = arrayListOf<String>(
            "assets/textures/CubeMap/left.png",
            "assets/textures/CubeMap/right.png",
            "assets/textures/CubeMap/bottom.png",
            "assets/textures/CubeMap/top.png",
            "assets/textures/CubeMap/back.png",
            "assets/textures/CubeMap/front.png"
        )

        skyboxTex = CubeMap(cubeFaces, false)
        skyboxTex.setTexParams()

        /* CubeMap - Skybox */
        val skyboxVBO = floatArrayOf(
            // pos, pos, pos
            -1.0f,  1.0f, -1.0f,//0
            -1.0f, -1.0f, -1.0f,//1
            1.0f, -1.0f, -1.0f,//2
            1.0f,  1.0f, -1.0f,//3
            -1.0f, -1.0f,  1.0f,//4
            -1.0f,  1.0f,  1.0f,//5
            1.0f, -1.0f,  1.0f,//6
            1.0f,  1.0f,  1.0f//7
        )
        val skyboxIBO = intArrayOf(
            0, 1, 2,
            2, 3, 0,
            4, 1, 0,
            0, 5, 4,
            2, 6, 7,
            7, 3, 2,
            4, 5, 7,
            7, 6, 4,
            0, 3, 7,
            7, 5, 0,
            1, 4, 2,
            2, 4, 6
        )

        val attributePosSky : VertexAttribute = VertexAttribute(3, GL_FLOAT, 12, 0)
        val skyboxAttributes = arrayOf(attributePosSky)

        val skyboxMesh = Mesh(skyboxVBO, skyboxIBO, skyboxAttributes, null)
        skybox = Renderable(mutableListOf(skyboxMesh))

        /** renderables [modelMatrix convention => T * R * S] **/
        val loader = loadOBJ("assets/models/ground.obj")
        ground = Renderable()
        for (m in loader.objects[0].meshes) {
            ground.meshes.add(
                Mesh(m.vertexData, m.indexData, vertexAttribs1, groundMaterial)
            )
        }
        ground.resetModelMatrixTo(Vector3f(0.0f))

        ball = loadModel("assets/models/ball.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        ball.resetModelMatrixTo(Vector3f(0.0f, 1.0f, 22.0f))
        ball.scale(Vector3f(0.8f, 0.8f, 0.8f))


        wall = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall.rotateAroundPoint(90.0f * (PI.toFloat()/180.0f), -90.0f * (PI.toFloat()/180.0f), 0.0f, Vector3f(0.0f))
        wall.rotationInDegree = 90.0f
        wall.preTranslate(Vector3f(-8.0f,0.0f,-15.0f))

        wall2 = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall2.rotateAroundPoint(0.0f, 0.0f, -90.0f * (PI.toFloat()/180.0f), Vector3f(0.0f))
        wall2.preTranslate(Vector3f(-9.0f,0.0f,-13.0f))

        wall3 = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall3.rotateAroundPoint(0.0f, 0.0f, -90.0f * (PI.toFloat()/180.0f), Vector3f(0.0f))
        wall3.preTranslate(Vector3f(-18.0f,0.0f,9.0f))

        wall4 = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall4.rotateAroundPoint(90.0f * (PI.toFloat()/180.0f), -90.0f * (PI.toFloat()/180.0f), 0.0f, Vector3f(0.0f))
        wall4.rotationInDegree = 90.0f
        wall4.preTranslate(Vector3f(-8.0f,0.0f,11.0f))

        wall5 = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall5.rotateAroundPoint(90.0f * (PI.toFloat()/180.0f), -90.0f * (PI.toFloat()/180.0f), 0.0f, Vector3f(0.0f))
        wall5.rotationInDegree = 90.0f
        wall5.preTranslate(Vector3f(7.0f,0.0f,4.0f))

        wall6 = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall6.rotateAroundPoint(0.0f, 0.0f, -90.0f * (PI.toFloat()/180.0f), Vector3f(0.0f))
        wall6.preTranslate(Vector3f(9.0f,0.0f,-6.0f))

        wall7 = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall7.rotateAroundPoint(90.0f * (PI.toFloat()/180.0f), -90.0f * (PI.toFloat()/180.0f), 0.0f, Vector3f(0.0f))
        wall7.rotationInDegree = 90.0f
        wall7.preTranslate(Vector3f(13.0f,0.0f,-7.0f))

        wall8 = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall8.rotateAroundPoint(0.0f, 0.0f, -90.0f * (PI.toFloat()/180.0f), Vector3f(0.0f))
        wall8.preTranslate(Vector3f(16.0f,0.0f,-23.0f))

        wall9 = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall9.rotateAroundPoint(0.0f, 0.0f, -90.0f * (PI.toFloat()/180.0f), Vector3f(0.0f))
        wall9.preTranslate(Vector3f(18.0f,0.0f,9.0f))

        wall10 = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall10.rotateAroundPoint(90.0f * (PI.toFloat()/180.0f), -90.0f * (PI.toFloat()/180.0f), 0.0f, Vector3f(0.0f))
        wall10.rotationInDegree = 90.0f
        wall10.preTranslate(Vector3f(7.0f,0.0f,18.0f))

        // ############################################################################################# //

        /** camera **/
        camera = TronCamera(
            custom(window.framebufferWidth, window.framebufferHeight),
            Math.toRadians(90.0f),
            0.1f,
            100.0f
        )
        //camera.translate(Vector3f(8.0f, 3.0f, 10.0f))
        camera.preTranslate(Vector3f(0.0f, 3.0f, 24.0f))
        camera.rotate(Math.toRadians(-35.0f), 0.0f, 0.0f)

        // ############################################################################################# //

        /** Finish Line **/

        finishLine = loadModel("assets/models/finishLine.obj", Math.toRadians(0.0f), Math.toRadians(0.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        finishLine.resetModelMatrixTo(Vector3f(0.0f, 0.0f, 0.0f))
        finishLine.translate(Vector3f(0.0f,0.0f,-20.0f))
        finishLine.scale(Vector3f(2.0f, 3.0f, 1.5f))

        // ############################################################################################# //

        /** additional uniforms **/
        groundColor = Vector3f(0.0f, 1.0f, 0.0f)

        pointLightList = mutableListOf()
        pointLightList.add(PointLight("pointLight[${pointLightList.size}]", Vector3f(0.0f, 2.0f, 2.0f), Vector3f(-10.0f, 2.0f, -10.0f)))
        pointLightList.add(PointLight("pointLight[${pointLightList.size}]", Vector3f(2.0f, 0.0f, 0.0f), Vector3f(10.0f, 2.0f, 10.0f)))

        spotLightList = mutableListOf()
        spotLightList.add(SpotLight("spotLight[${spotLightList.size}]", Vector3f(10.0f, 300.0f, 300.0f), Vector3f(6.0f, 2.0f, 4.0f), Math.toRadians(20.0f), Math.toRadians(30.0f)))
        spotLightList.last().rotate(Math.toRadians(20f), Math.toRadians(60f), 0f)

        // ############################################################################################# //

        /** OpenGL global settings **/

        glEnable(GL_DEPTH_TEST); GLError.checkThrow()
        glDepthFunc(GL_LESS); GLError.checkThrow()
    }

    fun render(dt: Float, t: Float) {
         /** per frame setup **/
         glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
         val changingColor = Vector3f(1.0f, 1.0f, 1.0f)

         // ############################################################################################# //

        /* skybox */
        glDepthFunc(GL_LEQUAL)
        skyShader.use()
        skyShader.setUniform("view_matrix", skyboxRotator.getModelMatrix(), false)
        skyShader.setUniform("projection_matrix", Matrix4f(), false)
        skyboxTex.bind(0, skyShader)
        skybox.render(skyShader)
        glDepthFunc(GL_LESS)

         /** selecting shader program **/
         staticShader.use()

         /** binding to current shader program **/
         staticShader.setUniform("shadingColor", groundColor)
         staticShader.setUniform("shadingColor", changingColor)
         camera.bind(staticShader)

         staticShader.setUniform("numPointLights", pointLightList.size)
         for (pointLight in pointLightList) {
            pointLight.bind(staticShader)
         }

         staticShader.setUniform("numSpotLights", spotLightList.size)
         for (spotLight in spotLightList) {
            spotLight.bind(staticShader, camera.calculateViewMatrix())
         }

         /** drawing with current shader program **/
         ground.render(staticShader)
         ball.render(staticShader)
         wall.render(staticShader)
         wall2.render(staticShader)
         wall3.render(staticShader)
         wall4.render(staticShader)
         wall5.render(staticShader)
         wall6.render(staticShader)
         wall7.render(staticShader)
         wall8.render(staticShader)
         wall9.render(staticShader)
         wall10.render(staticShader)
         finishLine.render(staticShader)
    }

    fun findNearRenderablesOf(player: Renderable,
                              radius: Float,
                              renderables: MutableList<Renderable>): MutableList<Renderable> {
        val validRenderables = mutableListOf<Renderable>()
        val playerPos = Vector2f(player.getPosition().x,player.getPosition().z)

        for (renderable in renderables) {
            val renderablePos = Vector2f(renderable.getPosition().x,renderable.getPosition().z)
            val vecPlayerToRenderable = Vector2f(renderablePos.x-playerPos.x,renderablePos.x-playerPos.x)

            if (vecPlayerToRenderable.length() <= radius){
                validRenderables.add(renderable)
            }
        }
        return validRenderables
    }

    fun isColliding(player: Renderable,
                    playerRadius: Float,
                    obstacle: Renderable,
                    possibleTranslationOfPlayer: Vector3f,
                    halfWidth : Float,
                    halfHeight : Float) : Boolean{

        val possiblePosition = Vector2f(player.getPosition().x + possibleTranslationOfPlayer.x,
                                        player.getPosition().z + possibleTranslationOfPlayer.z)
        val vecObstacleToPlayer = Vector2f(possiblePosition.x - obstacle.getPosition().x,
                                           possiblePosition.y - obstacle.getPosition().z)

        val clampedX: Float = min(vecObstacleToPlayer.x.absoluteValue, halfWidth)
        val clampedZ: Float = min(vecObstacleToPlayer.y.absoluteValue, halfHeight)

        var closestPoint = Vector2f(0.0f)

        if(clampedX == 0.0f || clampedZ == 0.0f){
            if(clampedX == 0.0f && vecObstacleToPlayer.y >= 0 ) closestPoint = Vector2f(0.0f, clampedZ)
            else if(clampedX == 0.0f && vecObstacleToPlayer.y < 0) closestPoint = Vector2f(0.0f, -clampedZ)
            else if(clampedZ == 0.0f && vecObstacleToPlayer.x >= 0) closestPoint = Vector2f(clampedX, 0.0f)
            else if(clampedZ == 0.0f && vecObstacleToPlayer.x < 0) closestPoint = Vector2f(-clampedX, 0.0f)
        } else {
            if (vecObstacleToPlayer.x > 0 && vecObstacleToPlayer.y > 0) closestPoint = Vector2f(clampedX, clampedZ)
            if (vecObstacleToPlayer.x < 0 && vecObstacleToPlayer.y > 0) closestPoint = Vector2f(-clampedX, clampedZ)
            if (vecObstacleToPlayer.x > 0 && vecObstacleToPlayer.y < 0) closestPoint = Vector2f(clampedX, -clampedZ)
            if (vecObstacleToPlayer.x < 0 && vecObstacleToPlayer.y < 0) closestPoint = Vector2f(-clampedX, -clampedZ)
        }

        val absClosestPointToPlayer = closestPoint.sub(vecObstacleToPlayer).length()
        return playerRadius > absClosestPointToPlayer
    }

    fun isInSquare(length : Float,midPoint : Vector2f , Player : Renderable) : Boolean{
        var playerPos = Vector2f(Player.getPosition().x, Player.getPosition().z)
        return ((midPoint.x - length) < playerPos.x  && playerPos.x < (midPoint.x + length)
             && (midPoint.y - length) < playerPos.y  && playerPos.y < (midPoint.y + length))
    }

    fun reset(){
         ball.resetModelMatrixTo(Vector3f(0.0f, 1.0f, 22.0f))
         ball.scale(Vector3f(0.8f, 0.8f, 0.8f))

         camera.resetModelMatrixTo(Vector3f(0.0f, 3.0f, 24.0f))
         camera.rotate(Math.toRadians(-35.0f), 0.0f, 0.0f)
    }

    fun update(dt: Float, t: Float) {
        val moveMul = 5.0f
        val rotateMul = 0.5f * Math.PI.toFloat()
        val listOfNearRenderables = findNearRenderablesOf(ball, 15.0f,mutableListOf(wall,wall2, wall3,wall4,wall5, wall6,wall7,wall8, wall9, wall10))
        var isKollision = false

        if (!isInSquare(23.0f,Vector2f(0.0f,0.0f) ,ball)) reset()
        if (isInSquare(0.5f, Vector2f(finishLine.getPosition().x,finishLine.getPosition().z),ball)) reset()

        if (window.getKeyState(GLFW_KEY_W)) {
            for(renderable in listOfNearRenderables){
                if (renderable.rotationInDegree.mod(180.0f) == 0.0f){
                    if (isColliding(ball,0.8f,renderable,Vector3f(0.0f,0.0f,dt * -moveMul),1.0f,10.0f)) {
                        isKollision = true
                        break
                    }
                } else {
                    if (isColliding(ball,0.8f,renderable,Vector3f(0.0f,0.0f,dt * -moveMul),10.0f,1.0f)) {
                        isKollision = true
                        break
                    }
                }

            }

            if (!isKollision) {
                ball.preTranslate(Vector3f(0.0f, 0.0f, dt * -moveMul))
                camera.preTranslate(Vector3f(0.0f, 0.0f, dt * -moveMul))
                ball.rotateAroundPoint(-dt * 3.5f, 0.0f,0.0f, ball.getPosition())
            }
        }
        if (window.getKeyState(GLFW_KEY_S)) {
            for(renderable in listOfNearRenderables){
                if (renderable.rotationInDegree.mod(180.0f) == 0.0f){
                    if (isColliding(ball,0.8f,renderable,Vector3f(0.0f,0.0f,dt * moveMul),1.0f,10.0f)) {
                        isKollision = true
                        break
                    }
                } else {
                    if (isColliding(ball,0.8f,renderable,Vector3f(0.0f,0.0f,dt * moveMul),10.0f,1.0f)) {
                        isKollision = true
                        break
                    }
                }
            }

            if (!isKollision) {
                ball.preTranslate(Vector3f(0.0f, 0.0f, dt * moveMul))
                camera.preTranslate(Vector3f(0.0f, 0.0f, dt * moveMul))
                ball.rotateAroundPoint(dt * 3.5f, 0.0f,0.0f, ball.getPosition())
            }
        }
        if (window.getKeyState(GLFW_KEY_A)) {
            for(renderable in listOfNearRenderables){
                if (renderable.rotationInDegree.mod(180.0f) == 0.0f){
                    if (isColliding(ball,0.8f,renderable,Vector3f(dt * -moveMul, 0.0f,0.0f),1.0f,10.0f)) {
                        isKollision = true
                        break
                    }
                } else {
                    if (isColliding(ball,0.8f,renderable,Vector3f(dt * -moveMul,0.0f,0.0f),10.0f,1.0f)) {
                        isKollision = true
                        break
                    }
                }
            }

            if (!isKollision) {
                ball.preTranslate(Vector3f(dt * -moveMul,0.0f, 0.0f))
                camera.preTranslate(Vector3f(dt * -moveMul,0.0f, 0.0f))
                ball.rotateAroundPoint(0.0f, 0.0f, dt * 3.5f, ball.getPosition())
            }
        }
        if (window.getKeyState(GLFW_KEY_D)) {
            for(renderable in listOfNearRenderables){
                if (renderable.rotationInDegree.mod(180.0f) == 0.0f){
                    if (isColliding(ball,0.8f,renderable,Vector3f(dt * moveMul,0.0f,0.0f),1.0f,10.0f)) {
                        isKollision = true
                        break
                    }
                } else {
                    if (isColliding(ball,0.8f,renderable,Vector3f(dt * moveMul,0.0f,0.0f),10.0f,1.0f)) {
                        isKollision = true
                        break
                    }
                }
            }

            if (!isKollision) {
                ball.preTranslate(Vector3f(dt * moveMul,0.0f, 0.0f))
                camera.preTranslate(Vector3f(dt * moveMul,0.0f, 0.0f))
                ball.rotateAroundPoint(0.0f, 0.0f, -dt * 3.5f, ball.getPosition())
            }
        }
    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {
         /** resetting ball and camera **/
         if (key == GLFW_KEY_R) {
            reset()
         }
    }

    fun onMouseMove(xpos: Double, ypos: Double) {
        if (!firstMouseMove) {
            val yawAngle = (xpos - oldMouseX).toFloat() * 0.002f
            val pitchAngle = (ypos - oldMouseY).toFloat() * 0.0005f
            if (!window.getKeyState(GLFW_KEY_LEFT_ALT)) {
                camera.rotateAroundPoint(0.0f, -yawAngle, 0.0f, ball.getPosition())
            }
            else{
                camera.rotateAroundPoint(0.0f, -yawAngle, 0.0f, ball.getPosition())
            }
        } else firstMouseMove = false
        oldMouseX = xpos
        oldMouseY = ypos
    }

    fun cleanup() {}

    fun onMouseScroll(xoffset: Double, yoffset: Double) {
        camera.fov += Math.toRadians(yoffset.toFloat())
    }
}