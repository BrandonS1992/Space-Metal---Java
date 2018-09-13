package space.metal;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import space.metal.classes.EntityA;
import space.metal.classes.EntityB;


/**
 *
 * @author Brandon Santangelo
 */
public class SpaceMetal extends Canvas implements Runnable {

    //Frame sizes to be called and HEIGHT = WIDTH is the aspect ration. 16*9 is widescreen. Scale is multiplication of aspect ratio window size.
    //private static final long serialVerisonUID = 1L; do I need this?
    public static final int WIDTH = 480;
    public static final int HEIGHT = WIDTH / 12 * 9;
    public static final int SCALE = 2;
    public final String TITLE = "Space Metal";
    
    private boolean running = false;
    private Thread thread;
    
    //Image buffer.
    private BufferedImage image = new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
    private BufferedImage spriteSheet = null;
    private BufferedImage background = null;
    private BufferedImage backgroundMenu = null;
    
    private boolean is_shooting = false;
    
    private int enemy_count = 5;
    private int enemy_killed = 0;
            
    private Player p;
    private Controller c;
    private Textures textures;
    private Menu menu;
    
    
    public LinkedList<EntityA> ea;
    public LinkedList<EntityB> eb;
    
    public static int HEALTH = 100 * 2;
    
    public static enum STATE{
        MENU,
        GAME
    };
    
    public static STATE state = STATE.MENU;
    
public void init(){
        //requestFocus makes the mouse click on screen when app starts automatically.
        requestFocus();
        BufferedImageLoader loader = new BufferedImageLoader();
        try{
            spriteSheet = loader.loadImage("/sprite_sheet.png");
            backgroundMenu = loader.loadImage("/backgroundMenu.png");
            background = loader.loadImage("/background.png");
        }catch(IOException e){
        }
        
        //this refers to this class.
        textures = new Textures(this);
        c = new Controller(textures, this);
        p = new Player(200, 200, textures, this, c);
        menu = new Menu();
        
        ea = c.getEntityA();
        eb = c.getEntityB();
        
        this.addKeyListener(new KeyInput(this));
        this.addMouseListener(new MouseInput());
        
        c.createEnemyFighter(enemy_count);
        
    }
    
    //synchronized prevents corruptions of multiple threads changing at once. Allows only one at a time.
    private synchronized void start(){
        if(running)
            return;
        
        running = true;
        thread = new Thread(this);
        thread.start();
    }
    
    //running is active, ! means not. !running means not running.
    private synchronized void stop(){ 
        if(!running)
            return;
        
        running = false;
        try {
            thread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(SpaceMetal.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.exit(1);
    }
    
    //Game loop.
    public void run(){
        init();
        long lastTime = System.nanoTime();
        final double framesPerSecond = 60.0;
        double ns = 1000000000 / framesPerSecond;
        double timePassed = 0;
        int updates = 0;
        int frames = 0;
        long timer = System.currentTimeMillis();
        
        while(running){
            long now = System.nanoTime();
            timePassed += (now - lastTime) / ns;
            lastTime = now;
            if(timePassed >= 1){
                frameTick();
                updates++;
                timePassed--;
            }
            render();
            frames++;
            
            if(System.currentTimeMillis() - timer > 1000){
                timer += 1000;
                System.out.println(updates + " frameTick, FPS " + frames);
                updates = 0;
                frames = 0;
            }
            
        }
        stop();
    }
    
    private void frameTick(){
        if(state == STATE.GAME){
            p.frameTick();
            c.frameSpeed();
        }
        
        if(enemy_killed >= enemy_count)
        {
            enemy_count += 2;
            enemy_killed = 0;
            c.createEnemyFighter(enemy_count);
        }
    }
    
    private void render(){
        BufferStrategy bs = this.getBufferStrategy();
        if(bs == null){
            //3 means it is creating 3 buffers of the image to be transferred to be displayed. 3 increases performance above 2.
            createBufferStrategy(3);
            return;
        }
        Graphics g = bs.getDrawGraphics();
        ////anything in between can draw out stuff////
        
        g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        
        g.drawImage(backgroundMenu, 0, 0, null);
        
        if(state == STATE.GAME){
        g.drawImage(background, 0, 0, null);
        p.render(g);
        c.render(g);
        
        //Health bar empty
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(5, 5, 200, 40);
        //Health bar full
        g.setColor(Color.green);
        g.fillRect(5, 5, HEALTH, 40);
        //Health bar border color
        g.setColor(Color.white);
        g.drawRect(5, 5, 200, 40);
        
        }else if(state == STATE.MENU){
            menu.render(g);
        }
        
        ////                                      ////
        g.dispose();
        bs.show();
    }
    
    public void keyPressed(KeyEvent e){
        int key = e.getKeyCode();
        //p.setX(p.getX() + 5); for choppy movement where p.setVelX(5); is
        
        if(state == STATE.GAME){
        if(key == KeyEvent.VK_RIGHT){
            p.setVelX(8);
        }else if (key == KeyEvent.VK_LEFT){
            p.setVelX(-8);
        }else if (key == KeyEvent.VK_DOWN){
            p.setVelY(8);
        }else if (key == KeyEvent.VK_UP){
            p.setVelY(-8);
            //! is short for not. Rather than is_shooting == false. True is simply is_shooting. True automatically.
        }else if (key == KeyEvent.VK_SPACE && !is_shooting){
            is_shooting = true;
            c.addEntity(new Bullet(p.getX(), p.getY(), textures, this));
        }
        }
    }
    public void keyReleased(KeyEvent e){
        int key = e.getKeyCode();
    
        if(key == KeyEvent.VK_RIGHT){
            p.setVelX(0);
        }else if (key == KeyEvent.VK_LEFT){
            p.setVelX(0);
        }else if (key == KeyEvent.VK_DOWN){
            p.setVelY(0);
        }else if (key == KeyEvent.VK_UP){
            p.setVelY(0);
        }else if(key == KeyEvent.VK_SPACE){
            is_shooting = false;
        }
    }
    
    public static void main(String args[]){
        SpaceMetal spaceGame = new SpaceMetal();
        
        //Setting window size by calling variables that are set above.
        spaceGame.setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        spaceGame.setMaximumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        spaceGame.setMinimumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
    
        JFrame gameFrame = new JFrame(spaceGame.TITLE);
        gameFrame.add(spaceGame);
        gameFrame.pack();
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setResizable(false);
        gameFrame.setLocationRelativeTo(null);
        gameFrame.setVisible(true);

        spaceGame.start();
    }    
    
    public BufferedImage getSpriteSheet(){
        return spriteSheet;
    }

    public int getEnemy_count() {
        return enemy_count;
    }

    public void setEnemy_count(int enemy_count) {
        this.enemy_count = enemy_count;
    }

    public int getEnemy_killed() {
        return enemy_killed;
    }

    public void setEnemy_killed(int enemy_killed) {
        this.enemy_killed = enemy_killed;
    }
    
    
    
    
}
