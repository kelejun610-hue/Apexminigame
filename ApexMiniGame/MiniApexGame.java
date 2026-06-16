import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class MiniApexGame extends JPanel implements ActionListener, KeyListener, MouseListener {
    
    // 玩家設定 (50血, 50甲)
    private int px = 150, py = 150, pSpeed = 4; 
    private int health = 50, shield = 50; 
    private boolean up, down, left, right;

    // 玩家虛空
    private boolean isVoid = false;
    private int voidTimer = 0;
    private int voidCooldown = 0; 

    // 玩家拉大電
    private boolean bIsHealing = false;
    private int healTimer = 0;
    private final int HEAL_DURATION = 180; 

    // 毒圈設定
    private int zoneRadius = 550;
    private int zoneShrinkTimer = 0; 
    private int zoneDamageTimer = 0; 

    // 遊戲狀態
    private boolean isWeaponSelected = false; 
    private int playerWeapon = -1;            
    private int smgBurstCount = 0;            
    private double smgAngle = 0;              
    private int smgTimer = 0;                 
    private boolean isGameOver = false;
    private boolean isVictory = false; 

    // 陣列清單
    private ArrayList<int[]> bullets = new ArrayList<>();       
    private ArrayList<int[]> enemyBullets = new ArrayList<>();  
    private ArrayList<int[]> enemies = new ArrayList<>();       
    private ArrayList<Rectangle> obstacles = new ArrayList<>(); 
    private ArrayList<int[]> gibbyDomes = new ArrayList<>();

    private int enemyIdCounter = 1; 

    // 小隊定義：0 = 老鷹 (玩家這隊), 1 = 獅子, 2 = 蛇, 3 = 鯊魚
    private final String[] TEAM_NAMES = {"老鷹小隊", "獅子小隊", "蛇小隊", "鯊魚小隊"};
    private int[] teamCounts = new int[4]; 

    public MiniApexGame() {
        this.setFocusable(true);
        this.addKeyListener(this);
        this.addMouseListener(this);
        
        // 11 個戰術掩體
        obstacles.add(new Rectangle(550, 400, 120, 120)); 
        obstacles.add(new Rectangle(350, 220, 150, 60));  
        obstacles.add(new Rectangle(250, 350, 60, 120));  
        obstacles.add(new Rectangle(800, 200, 70, 160));  
        obstacles.add(new Rectangle(870, 310, 120, 60));  
        obstacles.add(new Rectangle(450, 680, 300, 50));  
        obstacles.add(new Rectangle(500, 150, 200, 50));  
        obstacles.add(new Rectangle(150, 550, 90, 90));   
        obstacles.add(new Rectangle(950, 550, 90, 80));   
        obstacles.add(new Rectangle(400, 750, 80, 60));   
        obstacles.add(new Rectangle(980, 150, 70, 70));   

        initGame(); 
        new Timer(16, this).start(); 
    }

    private void initGame() {
        px = 150; py = 150; 
        health = 50; shield = 50; 
        zoneRadius = 550;
        zoneShrinkTimer = 0;
        zoneDamageTimer = 0;
        isVoid = false;
        voidTimer = 0;
        voidCooldown = 0;
        bIsHealing = false;
        healTimer = 0;
        isGameOver = false;
        isVictory = false; 
        isWeaponSelected = false; 
        playerWeapon = -1;
        smgBurstCount = 0;
        up = false; down = false; left = false; right = false;
        bullets.clear();
        enemyBullets.clear();
        enemies.clear();
        gibbyDomes.clear();
        
        enemyIdCounter = 1; 

        int[] spawnTeams = {0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3}; 

        for (int i = 0; i < spawnTeams.length; i++) {
            int ex, ey;
            boolean valid;
            int npcTeam = spawnTeams[i];
            int safetyCounter = 0; 

            do {
                valid = true;
                safetyCounter++;
                if (npcTeam == 0) {
                    ex = 150 + (int)(Math.random() * 80 - 40);
                    ey = 150 + (int)(Math.random() * 80 - 40);
                } else if (npcTeam == 1) {
                    ex = 1050 + (int)(Math.random() * 80 - 40);
                    ey = 150 + (int)(Math.random() * 80 - 40);
                } else if (npcTeam == 2) {
                    ex = 150 + (int)(Math.random() * 80 - 40);
                    ey = 750 + (int)(Math.random() * 80 - 40);
                } else {
                    ex = 1050 + (int)(Math.random() * 80 - 40);
                    ey = 750 + (int)(Math.random() * 80 - 40);
                }

                if (ex < 60 || ex > 1140 || ey < 60 || ey > 840) { valid = false; continue; }

                Rectangle spawnRect = new Rectangle(ex - 12, ey - 12, 24, 24);
                for (Rectangle rect : obstacles) {
                    if (spawnRect.intersects(rect)) { valid = false; break; }
                }
                
                if (safetyCounter > 100) { ex = 600; ey = 450; valid = true; break; }
            } while (!valid);

            int weaponType = (int)(Math.random() * 3); 

            enemies.add(new int[]{
                ex, ey, 
                0, // [2] 開槍計時器
                0, // [3] 打藥計時器
                50, // [4] 生命值 50
                weaponType, // [5] 兵種武器
                0, // [6] 特殊狀態計時器
                (int)(Math.random() * 150), // [7] 技能冷卻
                (Math.random() < 0.4 ? 1 : 0), // [8] 戰術風格
                enemyIdCounter++, // [9] 獨立ID
                0, // [10] 打藥狀態
                npcTeam, // [11] 小隊編號
                50, // [12] 護盾值 50
                0,  // [13] 獨立掩體計時器
                0   // [14] 備用
            });
        }
        updateTeamCounts(); 
    }

    private void updateTeamCounts() {
        teamCounts[0] = (health > 0) ? 1 : 0; 
        teamCounts[1] = 0;
        teamCounts[2] = 0;
        teamCounts[3] = 0;
        for (int[] e : enemies) {
            int team = e[11];
            if (team >= 0 && team < 4 && e[4] > 0) {
                teamCounts[team]++;
            }
        }
    }

    private void takeDamage(int damage) {
        if (bIsHealing) { 
            bIsHealing = false;
            healTimer = 0;
        }
        if (shield > 0) {
            shield -= damage;
            if (shield < 0) { health += shield; shield = 0; }
        } else {
            health -= damage;
        }
        if (health <= 0) { health = 0; isGameOver = true; updateTeamCounts(); }
    }

    private void npcTakeDamage(int[] npc, int damage, boolean isFriendly) {
        int finalDmg = isFriendly ? 1 : damage; 
        int curShield = npc[12];
        int curHealth = npc[4];

        if (curShield > 0) {
            curShield -= finalDmg;
            if (curShield < 0) { curHealth += curShield; curShield = 0; }
        } else {
            curHealth -= finalDmg;
        }

        npc[12] = curShield;
        npc[4] = curHealth;

        if (npc[10] == 1 && npc[4] > 0) npc[10] = 0; 
    }

    private boolean isInAnyDome(int x, int y) {
        for (int[] dome : gibbyDomes) {
            double dist = Math.sqrt(Math.pow(x - dome[0], 2) + Math.pow(y - dome[1], 2));
            if (dist <= 40) return true; 
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (!isWeaponSelected) {
            drawWeaponSelectionScreen(g);
            return;
        }

        // 1. 毒圈與安全區
        g.setColor(new Color(100, 30, 30)); g.fillRect(0, 0, 1200, 900);
        g.setColor(new Color(43, 48, 59)); g.fillOval(600 - zoneRadius, 450 - zoneRadius, zoneRadius * 2, zoneRadius * 2);
        g2d.setStroke(new BasicStroke(3)); g.setColor(Color.ORANGE); g.drawOval(600 - zoneRadius, 450 - zoneRadius, zoneRadius * 2, zoneRadius * 2);

        // 2. 繪製掩體
        for (Rectangle rect : obstacles) {
            g.setColor(new Color(90, 100, 110)); g.fillRect(rect.x, rect.y, rect.width, rect.height);
            g.setColor(new Color(130, 140, 150)); g2d.setStroke(new BasicStroke(2)); g.drawRect(rect.x, rect.y, rect.width, rect.height);
        }

        // 3. 罩子
        for (int[] dome : gibbyDomes) {
            g.setColor(new Color(0, 180, 255, 45)); g.fillOval(dome[0] - 40, dome[1] - 40, 80, 80);
            g.setColor(new Color(0, 220, 255)); g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6}, 0)); g.drawOval(dome[0] - 40, dome[1] - 40, 80, 80);
        }
        g2d.setStroke(new BasicStroke(2)); 

        // 4. 繪製所有 NPC
        for (int[] e : enemies) {
            int ex = e[0]; int ey = e[1]; int wp = e[5]; int isHealingNPC = e[10]; int tNum = e[11];

            if (wp == 1 && e[6] > 0) { 
                g.setColor(new Color(160, 80, 255, 80)); 
            } else {
                if (isHealingNPC == 1) g.setColor(Color.CYAN); 
                else if (tNum == 0) g.setColor(new Color(0, 255, 64));   
                else if (tNum == 1) g.setColor(new Color(255, 50, 50));  
                else if (tNum == 2) g.setColor(new Color(200, 50, 250)); 
                else g.setColor(new Color(255, 140, 0));                 
            }              

            g.fillOval(ex - 12, ey - 12, 24, 24); 
            
            if (wp == 2 && e[6] > 0) { 
                g.setColor(Color.GREEN); g2d.setStroke(new BasicStroke(2)); g.drawOval(ex - 15, ey - 15, 30, 30);
            }

            // 雙層狀態條
            g.setColor(Color.CYAN);  g.fillRect(ex - 12, ey - 24, (int)(24 * (e[12] / 50.0)), 3);
            g.setColor(Color.GREEN); g.fillRect(ex - 12, ey - 20, (int)(24 * (e[4] / 50.0)), 3);
            
            // 頭頂文字
            g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 11));
            if (isHealingNPC == 1) {
                g.setColor(Color.CYAN); g.fillRect(ex - 15, ey - 30, (int)(30 * ((e[3] % 180) / 180.0)), 3);
                g.drawString("💉 打藥中", ex - 16, ey - 35);
            } else {
                if (tNum == 0) {
                    g.setColor(new Color(0, 255, 64)); g.drawString("🤝【老鷹隊友】", ex - 32, ey - 29);
                } else {
                    g.setColor(Color.LIGHT_GRAY);
                    String clsName = wp == 0 ? "吉布" : (wp == 1 ? "惡靈" : "八仙");
                    g.drawString(TEAM_NAMES[tNum] + "(" + clsName + ")", ex - 35, ey - 29);
                }
            }
        }

        // 5. 繪製玩家自己
        if (isVoid) { g.setColor(new Color(0, 255, 255, 90)); g.fillOval(px - 16, py - 16, 32, 32); }
        else if (bIsHealing) { g.setColor(new Color(0, 200, 255)); g2d.setStroke(new BasicStroke(3)); g.drawOval(px - 15, py - 15, 30, 30); }
        g.setColor(Color.GREEN); g.fillOval(px - 12, py - 12, 24, 24); 
        g.setColor(Color.WHITE); g.fillOval(px - 3, py - 3, 6, 6);    
        g2d.setStroke(new BasicStroke(2));

        // 6. 子彈彈道
        g.setColor(Color.YELLOW);
        for (int[] b : bullets) g.drawLine(b[0], b[1], b[0] - (b[2] * 2), b[1] - (b[3] * 2));

        for (int[] eb : enemyBullets) {
            int et = eb[7]; 
            if (et == 0) g.setColor(Color.GREEN);
            else if (et == 1) g.setColor(Color.RED);
            else if (et == 2) g.setColor(Color.MAGENTA);
            else g.setColor(Color.ORANGE);
            g.drawLine(eb[0], eb[1], eb[0] - (eb[2] * 2), eb[1] - (eb[3] * 2));
        }

        // 7. 頂部介面
        g.setColor(new Color(15, 18, 26, 240));
        g.fillRect(360, 65, 480, 45);
        g.setColor(Color.ORANGE);
        g2d.setStroke(new BasicStroke(2));
        g.drawRect(360, 65, 480, 45);
        
        g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        g.setColor(new Color(50, 255, 50));   g.drawString("🦅 老鷹: " + teamCounts[0] + " 人", 380, 93);
        g.setColor(new Color(255, 80, 80));   g.drawString("🦁 獅子: " + teamCounts[1] + " 人", 495, 93);
        g.setColor(new Color(230, 80, 255));  g.drawString("🐍 蛇隊: " + teamCounts[2] + " 人", 610, 93);
        g.setColor(new Color(255, 160, 50));  g.drawString("🦈 鯊魚: " + teamCounts[3] + " 人", 725, 93);

        // 左側數據
        g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        g.setColor(Color.CYAN);  g.drawString("🛡️ 護盾值: " + shield + " / 50", 20, 30);
        g.setColor(Color.RED);   g.drawString("❤️ 生命值: " + health + " / 50", 20, 50);
        
        if (bIsHealing) {
            g.setColor(Color.CYAN); g.fillRect(20, 120, 150, 15);
            g.setColor(Color.WHITE); g.drawRect(20, 120, 150, 15);
            g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 11)); g.drawString("⚡ 正在拉電池... " + (healTimer * 100 / HEAL_DURATION) + "%", 25, 132);
        } else {
            g.setColor(Color.LIGHT_GRAY); g.drawString("⌨️ [4] 鍵：使用大盾電池 (需靜止3秒)", 20, 130);
        }

        String wpName = playerWeapon == 0 ? "霰彈槍" : (playerWeapon == 1 ? "衝鋒槍" : "手槍");
        g.setColor(Color.YELLOW); g.drawString("🔫 我的武器: " + wpName, 20, 150);
        g.setColor(Color.WHITE); g.drawString("🏆 陣營: 老鷹小隊隊長 (標準三人小隊)", 20, 170);
        
        if (isGameOver) {
            g.setColor(new Color(0, 0, 0, 220)); g.fillRect(0, 0, 1200, 900);
            g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 36)); g.setColor(Color.RED); g.drawString("戰場淘汰！", 520, 400);
            g.setColor(Color.WHITE); g.fillRect(500, 460, 200, 50);
            g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20)); g.setColor(Color.BLACK); g.drawString("重返戰場", 560, 492);
        }

        if (isVictory) {
            g.setColor(new Color(20, 25, 35, 230)); g.fillRect(0, 0, 1200, 900);
            g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 38)); g.setColor(Color.YELLOW); g.drawString("👑 帶領老鷹小隊奪得捍衛者！ 👑", 360, 380);
            g.setColor(Color.YELLOW); g.fillRect(500, 470, 200, 50);
            g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20)); g.setColor(Color.BLACK); g.drawString("再戰一場", 560, 502);
        }
    }

    private void drawWeaponSelectionScreen(Graphics g) {
        g.setColor(new Color(25, 28, 36)); g.fillRect(0, 0, 1200, 900);
        g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 36)); g.setColor(Color.ORANGE); g.drawString("小隊爭霸大逃殺：點擊選擇初始武器", 320, 300);
        g.setColor(new Color(180, 50, 50)); g.fillRect(350, 440, 140, 80);
        g.setColor(Color.WHITE); g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20)); g.drawString("霰彈槍", 390, 485);
        g.setColor(new Color(180, 130, 20)); g.fillRect(530, 440, 140, 80);
        g.setColor(Color.WHITE); g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20)); g.drawString("衝鋒槍", 570, 485);
        g.setColor(new Color(50, 140, 50)); g.fillRect(710, 440, 140, 80);
        g.setColor(Color.WHITE); g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20)); g.drawString("手槍", 760, 485);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isWeaponSelected || isGameOver || isVictory) return;

        for (int i = 0; i < gibbyDomes.size(); i++) {
            gibbyDomes.get(i)[2]--;
            if (gibbyDomes.get(i)[2] <= 0) { gibbyDomes.remove(i); i--; }
        }

        if (bIsHealing) {
            healTimer++;
            if (healTimer >= HEAL_DURATION) { shield = 50; bIsHealing = false; healTimer = 0; }
        }

        if (smgBurstCount > 0) {
            smgTimer++;
            if (smgTimer % 4 == 0) {
                int dx = (int) (Math.cos(smgAngle) * 12); int dy = (int) (Math.sin(smgAngle) * 12);
                bullets.add(new int[]{px, py, dx, dy, 2, 0}); 
                smgBurstCount--;
            }
        }

        // 玩家移動
        int oldX = px; int oldY = py;
        int speed = isVoid ? pSpeed * 2 : pSpeed;
        boolean moved = false;
        if (up) { py -= speed; moved = true; } if (down) { py += speed; moved = true; } 
        if (left) { px -= speed; moved = true; } if (right) { px += speed; moved = true; }

        if (moved && bIsHealing) { bIsHealing = false; healTimer = 0; }

        Rectangle playerRect = new Rectangle(px - 12, py - 12, 24, 24);
        for (Rectangle rect : obstacles) { if (playerRect.intersects(rect)) { px = oldX; py = oldY; break; } }

        if (isVoid) { voidTimer--; if (voidTimer <= 0) { isVoid = false; voidCooldown = 420; } }
        if (voidCooldown > 0 && !isVoid) voidCooldown--;

        if (zoneRadius > 60) { zoneShrinkTimer++; if (zoneShrinkTimer >= 12) { zoneRadius--; zoneShrinkTimer = 0; } }
        zoneDamageTimer++;
        boolean triggerZoneDamage = (zoneDamageTimer % 60 == 0);

        if (pDistToCenter() > zoneRadius && !isVoid && triggerZoneDamage) { 
            health -= 10; 
            if (bIsHealing) { bIsHealing = false; healTimer = 0; }
            if (health <= 0) { health = 0; isGameOver = true; updateTeamCounts(); }
        }

        // 建立臨時清除清單，杜絕死鎖
        ArrayList<int[]> deadBullets = new ArrayList<>();
        ArrayList<int[]> deadEnemyBullets = new ArrayList<>();
        ArrayList<int[]> deadEnemies = new ArrayList<>();

        // 玩家子彈邏輯
        for (int[] b : bullets) {
            int bOldX = b[0]; int bOldY = b[1];
            b[0] += b[2]; b[1] += b[3];
            
            boolean hitObject = false;
            for (Rectangle rect : obstacles) { 
                if (rect.contains(b[0], b[1])) { deadBullets.add(b); hitObject = true; break; } 
            }
            if (hitObject) continue;

            if (isInAnyDome(b[0], b[1]) != isInAnyDome(bOldX, bOldY)) { deadBullets.add(b); continue; }

            for (int j = 0; j < enemies.size(); j++) {
                int[] enemy = enemies.get(j);
                if (enemy[11] == 0 || deadEnemies.contains(enemy)) continue; 

                if (Math.sqrt(Math.pow(b[0] - enemy[0], 2) + Math.pow(b[1] - enemy[1], 2)) < 16) {
                    npcTakeDamage(enemy, b[4] * 4, false); 
                    deadBullets.add(b);
                    if (enemy[4] <= 0) deadEnemies.add(enemy);
                    break;
                }
            }
        }

        // 敵人與隊友子彈邏輯
        for (int[] eb : enemyBullets) {
            int ebOldX = eb[0]; int ebOldY = eb[1];
            eb[0] += eb[2]; eb[1] += eb[3];
            
            boolean hitObject = false;
            for (Rectangle rect : obstacles) { 
                if (rect.contains(eb[0], eb[1])) { deadEnemyBullets.add(eb); hitObject = true; break; } 
            }
            if (hitObject) continue;

            if (isInAnyDome(eb[0], eb[1]) != isInAnyDome(ebOldX, ebOldY)) { deadEnemyBullets.add(eb); continue; }

            int shooterTeam = eb[7]; 

            if (!isVoid && Math.sqrt(Math.pow(eb[0] - px, 2) + Math.pow(eb[1] - py, 2)) < 16) {
                if (shooterTeam != 0) { 
                    int dmg = (eb[5] == 0) ? 14 : ((eb[5] == 1) ? 6 : 9); 
                    takeDamage(dmg); 
                }
                deadEnemyBullets.add(eb); 
                continue;
            }

            for (int j = 0; j < enemies.size(); j++) {
                int[] tgt = enemies.get(j);
                if (deadEnemies.contains(tgt)) continue;

                if (tgt[9] != eb[6]) { 
                    if (Math.sqrt(Math.pow(eb[0] - tgt[0], 2) + Math.pow(eb[1] - tgt[1], 2)) < 16) {
                        int dmg = (eb[5] == 0) ? 14 : ((eb[5] == 1) ? 6 : 9);
                        boolean isFriendly = (tgt[11] == shooterTeam);
                        
                        npcTakeDamage(tgt, dmg, isFriendly);
                        deadEnemyBullets.add(eb);
                        if (tgt[4] <= 0) deadEnemies.add(tgt);
                        break;
                    }
                }
            }
        }

        // NPC AI 邏輯與毒圈死亡打標
        for (int j = 0; j < enemies.size(); j++) {
            int[] enemy = enemies.get(j);
            if (deadEnemies.contains(enemy)) continue;

            int ex = enemy[0]; int ey = enemy[1]; 
            int enemyWeaponType = enemy[5]; int combatStyle = enemy[8]; int isHealingNPC = enemy[10]; int myTeam = enemy[11];
            double eDistToCenter = Math.sqrt(Math.pow(ex - 600, 2) + Math.pow(ey - 450, 2));

            if (eDistToCenter > zoneRadius && triggerZoneDamage) {
                enemy[4] -= 5; 
                if (enemy[4] <= 0) { deadEnemies.add(enemy); continue; }
            }

            enemy[7]++; 
            int speedBonus = 0;
            
            if (enemyWeaponType == 0 && enemy[7] > 380 && isHealingNPC == 0) { gibbyDomes.add(new int[]{ex, ey, 240}); enemy[7] = 0; } 
            if (enemyWeaponType == 1 && enemy[6] > 0) { speedBonus = 2; enemy[6]--; }
            if (enemyWeaponType == 1 && enemy[6] <= 0 && enemy[7] > 300 && enemy[4] <= 15) { enemy[6] = 90; enemy[7] = 0; }
            if (enemyWeaponType == 2 && enemy[6] > 0) { speedBonus = 2; enemy[6]--; }
            if (enemyWeaponType == 2 && enemy[6] <= 0 && (enemy[7] > 220 || enemy[4] <= 20)) { enemy[6] = 100; enemy[7] = 0; }

            int targetX = px; int targetY = py; 
            boolean hasTarget = (myTeam != 0); 
            double minDist = Math.sqrt(Math.pow(ex - px, 2) + Math.pow(ey - py, 2));

            if (myTeam == 0) {
                minDist = Double.MAX_VALUE;
                for (int[] other : enemies) {
                    if (other[11] != 0 && !deadEnemies.contains(other)) { 
                        double d = Math.sqrt(Math.pow(ex - other[0], 2) + Math.pow(ey - other[1], 2));
                        if (d < minDist) { minDist = d; targetX = other[0]; targetY = other[1]; hasTarget = true; }
                    }
                }
            } else {
                if (Math.random() < 0.25) {
                    for (int[] other : enemies) {
                        if (other[11] != myTeam && !deadEnemies.contains(other)) { 
                            double d = Math.sqrt(Math.pow(ex - other[0], 2) + Math.pow(ey - other[1], 2));
                            if (d < minDist) { minDist = d; targetX = other[0]; targetY = other[1]; }
                        }
                    }
                }
            }

            if ((enemy[4] + enemy[12]) <= 30 && isHealingNPC == 0) { enemy[10] = 1; enemy[3] = 0; }

            int goalX = targetX; int goalY = targetY;
            boolean runToSafeZone = false;

            if (eDistToCenter > zoneRadius - 60) {
                goalX = 600; goalY = 450; runToSafeZone = true; 
            } else if (enemy[10] == 1) {
                goalX = 600; goalY = 450;
                enemy[3]++; 
                if (enemy[3] >= 180) { enemy[4] = 50; enemy[12] = 50; enemy[10] = 0; enemy[3] = 0; } 
            } else if (combatStyle == 1 && minDist < 300 && hasTarget) { 
                Rectangle bestCover = null; double minCoverDist = Double.MAX_VALUE;
                for (Rectangle rect : obstacles) {
                    double d = Math.sqrt(Math.pow(ex - (rect.x + rect.width/2), 2) + Math.pow(ey - (rect.y + rect.height/2), 2));
                    if (d < minCoverDist) { minCoverDist = d; bestCover = rect; }
                }
                if (bestCover != null) { enemy[13] = (enemy[13] + 1) % 150; if (enemy[13] < 55) { goalX = bestCover.x - 10; goalY = bestCover.y - 10; } }
            } else {
                if (minDist < 130 && hasTarget) { goalX = ex - (targetX - ex); goalY = ey - (targetY - ey); }
            }

            int currentESpeed = (enemy[10] == 1) ? 1 + speedBonus : 1 + speedBonus; 
            int moveX = 0; int moveY = 0;
            
            if (hasTarget) {
                if (Math.abs(ex - goalX) > currentESpeed) {
                    moveX = (ex < goalX) ? currentESpeed : -currentESpeed;
                } else {
                    enemy[0] = goalX; 
                }
                if (Math.abs(ey - goalY) > currentESpeed) {
                    moveY = (ey < goalY) ? currentESpeed : -currentESpeed;
                } else {
                    enemy[1] = goalY;
                }
            }

            int nextX = enemy[0] + moveX; int nextY = enemy[1] + moveY;
            
            if (nextX < 40) nextX = 40; if (nextX > 1160) nextX = 1160;
            if (nextY < 40) nextY = 40; if (nextY > 860) nextY = 860;

            Rectangle rectX = new Rectangle(nextX - 12, enemy[1] - 12, 24, 24);
            boolean collideX = false;
            for (Rectangle rect : obstacles) { if (rectX.intersects(rect)) { collideX = true; break; } }
            if (!collideX) enemy[0] = nextX;

            Rectangle rectY = new Rectangle(enemy[0] - 12, nextY - 12, 24, 24);
            boolean collideY = false;
            for (Rectangle rect : obstacles) { if (rectY.intersects(rect)) { collideY = true; break; } }
            if (!collideY) enemy[1] = nextY;

            if (runToSafeZone || enemy[10] == 1 || !hasTarget) continue; 

            // 開槍 (已修復：補上 enemyWeaponType 以防止陣列越界異常)
            enemy[2]++;
            if (enemyWeaponType == 0 && enemy[2] >= 130) { 
                enemy[2] = 0; double angle = Math.atan2(targetY - enemy[1], targetX - enemy[0]);
                enemyBullets.add(new int[]{enemy[0], enemy[1], (int)(Math.cos(angle)*5), (int)(Math.sin(angle)*5), 2, enemyWeaponType, enemy[9], myTeam}); 
            } else if (enemyWeaponType == 1 && enemy[2] >= 100) {
                if (enemyWeaponType == 1 && enemy[6] > 0) continue; 
                enemy[2] = 0; double baseAngle = Math.atan2(targetY - enemy[1], targetX - enemy[0]);
                for (int i = 0; i < 3; i++) { 
                    double offset = -0.12 + (i * 0.12); 
                    enemyBullets.add(new int[]{enemy[0], enemy[1], (int)(Math.cos(baseAngle+offset)*6), (int)(Math.sin(baseAngle+offset)*6), 2, enemyWeaponType, enemy[9], myTeam}); 
                }
            } else if (enemyWeaponType == 2 && enemy[2] >= 75) {
                enemy[2] = 0; double angle = Math.atan2(targetY - enemy[1], targetX - enemy[0]);
                enemyBullets.add(new int[]{enemy[0], enemy[1], (int)(Math.cos(angle)*7), (int)(Math.sin(angle)*7), 2, enemyWeaponType, enemy[9], myTeam}); 
            }
        }

        // 統一安全清除，全面杜絕 ConcurrentModificationException 與無窮死鎖
        bullets.removeAll(deadBullets);
        enemyBullets.removeAll(deadEnemyBullets);
        enemies.removeAll(deadEnemies);

        updateTeamCounts();

        if (teamCounts[1] == 0 && teamCounts[2] == 0 && teamCounts[3] == 0) { isVictory = true; repaint(); return; }

        repaint();
    }

    private double pDistToCenter() { return Math.sqrt(Math.pow(px - 600, 2) + Math.pow(py - 450, 2)); }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!isWeaponSelected || isGameOver || isVictory) return;
        if (e.getKeyCode() == KeyEvent.VK_W) up = true;
        if (e.getKeyCode() == KeyEvent.VK_S) down = true;
        if (e.getKeyCode() == KeyEvent.VK_A) left = true;
        if (e.getKeyCode() == KeyEvent.VK_D) right = true;
        if (e.getKeyCode() == KeyEvent.VK_SHIFT && !isVoid && voidCooldown == 0 && !bIsHealing) { isVoid = true; voidTimer = 120; }
        
        if (e.getKeyCode() == KeyEvent.VK_4 || e.getKeyCode() == KeyEvent.VK_NUMPAD4) {
            if (!isVoid && !bIsHealing && shield < 50) { bIsHealing = true; healTimer = 0; }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_W) up = false;
        if (e.getKeyCode() == KeyEvent.VK_S) down = false;
        if (e.getKeyCode() == KeyEvent.VK_A) left = false;
        if (e.getKeyCode() == KeyEvent.VK_D) right = false;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int mx = e.getX(); int my = e.getY();

        if (!isWeaponSelected) {
            if (mx >= 340 && mx <= 500 && my >= 430 && my <= 530) { playerWeapon = 0; isWeaponSelected = true; }
            else if (mx >= 520 && mx <= 680 && my >= 430 && my <= 530) { playerWeapon = 1; isWeaponSelected = true; }
            else if (mx >= 700 && mx <= 860 && my >= 430 && my <= 530) { playerWeapon = 2; isWeaponSelected = true; }
            repaint(); return;
        }

        if (isVictory && mx >= 500 && mx <= 700 && my >= 470 && my <= 540) { initGame(); repaint(); return; }
        if (isGameOver && mx >= 500 && mx <= 700 && my >= 460 && my <= 530) { initGame(); repaint(); return; }

        if (bIsHealing) { bIsHealing = false; healTimer = 0; }

        if (!isVoid) {
            double angle = Math.atan2(my - py, mx - px);
            if (playerWeapon == 0) { 
                for (double offset = -0.25; offset <= 0.25; offset += 0.12) { bullets.add(new int[]{px, py, (int)(Math.cos(angle+offset)*11), (int)(Math.sin(angle+offset)*11), 2}); }
            } 
            else if (playerWeapon == 1) { 
                if (smgBurstCount == 0) { smgBurstCount = 3; smgAngle = angle; smgTimer = 0; }
            } 
            else if (playerWeapon == 2) { 
                bullets.add(new int[]{px, py, (int)(Math.cos(angle)*13), (int)(Math.sin(angle)*13), 3}); 
            }
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Apex 聯賽：全面防禦完美流暢版");
        frame.add(new MiniApexGame());
        frame.setSize(1200, 930); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
