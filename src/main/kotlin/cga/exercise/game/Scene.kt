package cga.exercise.game

import cga.exercise.components.camera.Aspectratio.Companion.custom
import cga.exercise.components.camera.TronCamera
import cga.exercise.components.geometry.*
import cga.exercise.components.light.PointLight
import cga.exercise.components.light.SpotLight
import cga.exercise.components.shader.ShaderProgram
import cga.exercise.components.texture.Texture2D
import cga.framework.GLError
import cga.framework.GameWindow
import cga.framework.ModelLoader.loadModel
import cga.framework.OBJLoader.loadOBJ
import org.joml.Math
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*
import kotlin.math.*

class Scene(private val window: GameWindow) {

    /** Variable Declarations **/

    /** 1) shader programs **/
    private val staticShader: ShaderProgram
    /** 2) materials **/
    private val groundMaterial: Material
    /** 3) renderables **/
    private val ground: Renderable
    private val ball: Renderable
    private val wall: Renderable
    private val wall2: Renderable
    /** 4) camera **/
    private val camera: TronCamera
    private var oldMouseX       = 0.0
    private var oldMouseY       = 0.0
    private var firstMouseMove  = true
    /** 5) additional uniforms **/
    private val groundColor: Vector3f
    private val pointLightList: MutableList<PointLight>
    private val spotLightList: MutableList<SpotLight>

    /** Variable Definition/Initialisation **/
    init {
        /** shader programs **/
        staticShader = ShaderProgram("assets/shaders/tron_vert.glsl", "assets/shaders/tron_frag.glsl")

        // ############################################################################################# //

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
        ball.resetModelMatrixTo(Vector3f(8.0f, 1.0f, 8.0f))
        ball.scale(Vector3f(0.8f, 0.8f, 0.8f))

        wall = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall.resetModelMatrixTo(Vector3f(0.0f,0.0f,0.0f))
        wall.rotateAroundPoint(90.0f * (PI.toFloat()/180.0f), -90.0f * (PI.toFloat()/180.0f), 0.0f, Vector3f(0.0f))
        wall.rotationInDegree = 90.0f

        wall2 = loadModel("assets/models/Wall.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")
        wall2.resetModelMatrixTo(Vector3f(0.0f,0.0f,-3.0f))
        wall2.rotateAroundPoint(0.0f, 0.0f, -90.0f * (PI.toFloat()/180.0f), Vector3f(0.0f))

        // ############################################################################################# //

        /** camera **/
        camera = TronCamera(
            custom(window.framebufferWidth, window.framebufferHeight),
            Math.toRadians(90.0f),
            0.1f,
            100.0f
        )
        camera.translate(Vector3f(8.0f, 3.0f, 10.0f))
        camera.rotate(Math.toRadians(-35.0f), 0.0f, 0.0f)

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
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f); GLError.checkThrow()
        glEnable(GL_CULL_FACE); GLError.checkThrow()
        glFrontFace(GL_CCW); GLError.checkThrow()
        glCullFace(GL_BACK); GLError.checkThrow()
        glEnable(GL_DEPTH_TEST); GLError.checkThrow()
        glDepthFunc(GL_LESS); GLError.checkThrow()
    }

    fun render(dt: Float, t: Float) {
         /** per frame setup **/
         glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
         val changingColor = Vector3f(Math.abs(Math.sin(t)), 0.0f, Math.abs(Math.cos(t)))

         // ############################################################################################# //

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

    fun update(dt: Float, t: Float) {
        val moveMul = 5.0f
        val rotateMul = 0.5f * Math.PI.toFloat()
        val listOfNearRenderables = findNearRenderablesOf(ball, 15.0f,mutableListOf(wall,wall2))
        var isKollision = false

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
             ball.resetModelMatrixTo(Vector3f(8.0f, 1.0f, 8.0f))
             ball.scale(Vector3f(0.8f, 0.8f, 0.8f))

             camera.resetModelMatrixTo(Vector3f(8.0f, 3.0f, 10.0f))
             camera.rotate(Math.toRadians(-35.0f), 0.0f, 0.0f)
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