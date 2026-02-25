package u0012604.box2ddemo

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.assets.toInternalFile
import ktx.async.KtxAsync
import ktx.graphics.use
import u0012604.box2ddemo.actors.Gem
import u0012604.box2ddemo.actors.Player
import u0012604.box2ddemo.box2d.Box2dConstants
import u0012604.box2ddemo.box2d.buildBox2dWorld

class Main : KtxGame<KtxScreen>() {
    override fun create() {
        KtxAsync.initiate()

        addScreen(FirstScreen())
        setScreen<FirstScreen>()
    }
}

class FirstScreen : KtxScreen, GameLooper, ContactListener {
    val map = TmxMapLoader().load("Maps/TestMap.tmx")!!

    val world = buildBox2dWorld(
        map,
        Vector2(0f, -10f),
        "Collisions",
        Box2dConstants.SIMULATION_STEP,
        Box2dConstants.VELOCITY_ITERATIONS,
        Box2dConstants.POSITION_ITERATIONS,
        Box2dConstants.PIXELS_PER_METER
    ).apply {
        setContactListener(this@FirstScreen)
    }

    val player = Player(map, world)

    val gemTexture = Texture("Spritesheets/gems/crystal-crystal-qubodup-ccby3-32.png".toInternalFile())
    val gems = Gem.buildGemCollection(world, map, gemTexture)

    val camera = OrthographicCamera().apply {
        zoom = 0.25f
        update()
    }
    val viewport =
        FitViewport(
            Gdx.graphics.width / Box2dConstants.PIXELS_PER_METER,
            Gdx.graphics.height / Box2dConstants.PIXELS_PER_METER,
            camera)

    private val batch = SpriteBatch()

    val renderer = OrthogonalTiledMapRenderer(map, 1f / Box2dConstants.PIXELS_PER_METER)

    val debugBox2dRenderer = Box2DDebugRenderer()

    private var accumulator = 0f // Used for Box2d world updates - see https://libgdx.com/wiki/extensions/physics/box2d#stepping-the-simulation

    private var startDelay = 0f // just prevent anything from happening until this reaches a certain value!

    init {
        Gdx.app.log("FirstScreen", "There are ${gems.size} in the map")
    }

    // ---------------------------------------------------
    // doPhysics()
    // Update the world physics
    // ---------------------------------------------------
    private fun doPhysics(delta: Float) {
        startDelay += delta

        if(startDelay < 5) return

        // 1. Cap the delta time to avoid the "Spiral of Death".
        // If the game lags (e.g., a 2-second pause), we don't want Box2D to try
        // to catch up by running 100+ steps in a single frame.
        val frameTime = delta.coerceAtMost(0.25f)

        // 2. Add the time passed since the last frame to our "bank" (accumulator).
        accumulator += frameTime

        // 3. While we have enough time in our "bank" to run a full physics step...
        while(accumulator >= Box2dConstants.SIMULATION_STEP) {
            // 4. Step the physics world forward by a fixed, constant amount.
            // This ensures the physics math is deterministic and stable regardless of FPS.
            world.step(
                Box2dConstants.SIMULATION_STEP,
                Box2dConstants.VELOCITY_ITERATIONS,
                Box2dConstants.POSITION_ITERATIONS)

            // 5. Subtract the time we just "consumed" from the bank.
            accumulator -= Box2dConstants.SIMULATION_STEP
        }

        // Note: Any leftover time in the accumulator (less than SIMULATION_STEP)
        // stays there and is added to the next frame's delta.
    }

    // ---------------------------------------------------
    // render()
    // ---------------------------------------------------
    override fun render(delta: Float) {
//        clearScreen(red = 0.7f, green = 0.7f, blue = 0.7f)
        ScreenUtils.clear(0f, 0f, 0f, 1f)

        update(delta)

        draw()
    }

    // ---------------------------------------------------
    // input()
    // ---------------------------------------------------
    override fun input() {
    }

    // ---------------------------------------------------
    // update()
    // ---------------------------------------------------
    override fun update(delta: Float) {
        doPhysics(delta)

        // Follow the player
        camera.position.set(player.body?.position?.x ?: 0f, player.body?.position?.y ?: 0f, 0f)

//        Gdx.app.log("FirstScreen", "Player Position - ${player.body?.position?.x ?: 0f}, ${player.body?.position?.y ?: 0f}")

        // Ensure the camera is updated
        camera.update()

        // Update the gems - i.e. the physics
        gems.forEach { it.update() }

        // Update the player - i.e. the physics
        player.update()
    }

    // ---------------------------------------------------
    // draw()
    // ---------------------------------------------------
    override fun draw() {
        renderer.apply {
            setView(camera)

            render()
        }

        batch.use {
            it.projectionMatrix = camera.combined

            player.sprite.draw(it)

            gems.forEach { gem -> gem.sprite.draw(it) }
        }

        debugBox2dRenderer.render(world, camera.combined)
    }

    // ---------------------------------------------------
    // resize()
    // ---------------------------------------------------
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        // Update the viewport
        viewport.update(width, height)
    }

    // ---------------------------------------------------
    // dispose()
    // ---------------------------------------------------
    override fun dispose() {
        world.disposeSafely()
        player.disposeSafely()
        gemTexture.disposeSafely()
        map.disposeSafely()
        batch.disposeSafely()
    }

    // ---------------------------------------------------
    // beginContact()
    //
    // If you want to what has started contact
    // in Box2d world
    // ---------------------------------------------------
    override fun beginContact(contact: Contact?) {
//        Gdx.app.log("FirstScreen", "beginContact")

        contact?.let {
            Gdx.app.log("FirstScreen", "A collision has occurred! -- ${it.fixtureA.userData} : ${it.fixtureB.userData}")
            // When the player hits the ground, update its linear velocity ...
            if(it.fixtureA.userData is com.badlogic.gdx.physics.box2d.Body && it.fixtureB.userData == player) {
                player.body.linearVelocity = Vector2(8f, 0f)
            }
        }
    }

    // ---------------------------------------------------
    // endContact()
    //
    // If you want to what has ended contact
    // in Box2d world
    // ---------------------------------------------------
    override fun endContact(contact: Contact?) {
//        Gdx.app.log("FirstScreen", "endContact")
    }

    override fun preSolve(
        contact: Contact?,
        oldManifold: Manifold?
    ) {
//        Gdx.app.log("FirstScreen", "preSolve")
    }

    override fun postSolve(
        contact: Contact?,
        impulse: ContactImpulse?
    ) {
//        Gdx.app.log("FirstScreen", "postSolve")
    }
}


