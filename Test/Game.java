import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)
import java.util.*;
import java.lang.Math.*;
/**
 * This is the main class where the game is coded.
 * The data structures (Arrays,ArrayLists,HashMaps)are contained here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */

//feature suggestions: timed score achievement and unorthodox solution achievement
public class Game extends World{

    // Inner class to store the properties of each tile/block
    class Block{
        String item;
        Boolean solved;
    }
    public static GreenfootSound cursor = new GreenfootSound("Cursor.mp3");
    // Set particle count per letter
    private int NUM_OF_PARTICLES = 20;
    // Word length size is max at 17
    public static int wordLength;
    public static int numOfWords;

    private int blockSize;
    private int offSet;
    // Word Counter and Board Counter
    public static Counter counter = new Counter();
    public static Counter boardCounter = new Counter();
    // ArrayList of solved words,scores of words/boards
    private ArrayList<String> solvedwords = new ArrayList<String>();
    private ArrayList<Integer> wordCheck = new ArrayList<Integer>();
    private ArrayList<Integer> boardcheck = new ArrayList<Integer>();
    //HashMaps  that map a key(the score of the counter) to a value(achievement)
    private HashMap<Integer,GreenfootImage> boardAchievements = new HashMap<Integer,GreenfootImage>();
    private HashMap<Integer,GreenfootImage> wordAchievements = new HashMap<Integer,GreenfootImage>();
    private Button backtomenu = new Button(new GreenfootImage("BackToMenu-1.png"), getHeight()/15, 3.8);
    private boolean isDown = false;
    private boolean pause = false;
    private boolean hasWon = false;
    private boolean hasPaused = false;
    private int pauseOption = 1;
    private Random r = new Random();
    // selected will keep track of which Block is selected in each column
    private int[] selected;
    // Create the game board
    private ArrayList<String> words;
    private ArrayList<ArrayList<Block>> board;
    // selectedColumn will keep track of, who could've guesses, the selected column...
    private int selectedColumn = 0;
    public Game(){    
        super(1280, 720, 1);
        // Ask user for word length
        while(true){
            String input = Greenfoot.ask("How long would you like the words to be? (1 - 20 characters)");
            if(isNumeric(input)){
                int temp = (int) Double.parseDouble(input);
                if(temp > 0 && temp < 21){
                    wordLength = temp;
                    break;
                }
            }
        }

        // Ask user for num of words
        while(true){
            //if there are not enough words, there will be as many as possible
            String input = Greenfoot.ask("How many words would you like? (under 8 is recommended)");
            if(isNumeric(input)){
                int temp = (int) Double.parseDouble(input);
                if(temp > 1){
                    numOfWords = temp;
                    break;
                }

            }
        }

        // Initialize variables
        words = Reader.read(wordLength);
        selected = new int[wordLength];
        board = createBoard();

        if(wordLength > 13){
            offSet = getWidth()/(wordLength+2) + (getWidth()/(wordLength+2))/2;
            blockSize = getWidth()/(wordLength+2);
        }else {
            blockSize = (wordLength%2 == 0) ? getWidth()/14 : getWidth()/15;
            offSet = ((getWidth() - (blockSize*wordLength))/2) + (blockSize/2);
        }

        // Add achievement pop-ups into hashmap
        wordAchievements.put(1,new GreenfootImage("MyFirstWord-PopUp.png"));
        wordAchievements.put(5,new GreenfootImage("WordNerd-PopUp.png"));
        wordAchievements.put(8,new GreenfootImage("LetterWizard-PopUp.png"));
        boardAchievements.put(1,new GreenfootImage("BigBrainUser-PopUp.png"));
        boardAchievements.put(3,new GreenfootImage("SpellingBeeExpert-PopUp.png"));
        boardAchievements.put(5,new GreenfootImage("BoardMaster-PopUp.png"));
        boardAchievements.put(15,new GreenfootImage("Champion-Achieved.png"));
        // Check Achievements
        if(wordLength==1){
            Slide s = new Slide(new GreenfootImage("Trivial-PopUp.png"));
            addObject(s, 1050, 680);
            Greenfoot.delay(50);
        }
        if(wordLength > 13){
            Slide s = new Slide(new GreenfootImage("DeathWish-PopUp.png"));
            addObject(s, 1050, 680);
            Greenfoot.delay(50);
        }
        if(numOfWords >= 17){
            Slide s = new Slide(new GreenfootImage("WristDamage-PopUp.png"));
            addObject(s, 1050, 680);
            Greenfoot.delay(50);
        }

        // Fill selected with 0
        for(int i = 0; i < selected.length; i++){
            selected[i] = r.nextInt(board.get(i).size());
        }

        // Draw board
        String selectedStr = selectedString(board, selected);
        if(words.contains(selectedStr)){
            if(!solvedwords.contains(selectedStr)){
                TitleScreen.click.play();
                counter.add();
            }    
            solvedwords.add(selectedStr);
            for(int i = 0; i < selected.length; i++){
                board.get(i).get(selected[i]).solved = true;
                spawnParticles(offSet+blockSize*i, getHeight()/2);
            }
            List<Actor> actors = getObjects(null);
            actors.removeAll(getObjects(Particle.class));
            removeObjects(actors);
            drawBoard();
            for(int i = 0; i < selected.length; i++){
                spawnParticles(offSet+blockSize*i, getHeight()/2);
            }
        }else{
            List<Actor> actors = getObjects(null);
            actors.removeAll(getObjects(Particle.class));
            removeObjects(actors);
            drawBoard();
        }
        addObject(counter, 1080, 100);
        checkAchievements();
    }

    public void act(){
        setPaintOrder(Game.class, Particle.class);
        if(!hasPaused && !hasWon){
            showText("Press ENTER to pause", 200,670);
        }
        // Add counter
        addObject(counter, 1080, 100);
        

        if(Greenfoot.isKeyDown("enter") && !pause && !hasWon){
            removeObjects(getObjects(null));
            pauseOption = 1;
            drawPauseMenu();
            pause = true;
        }
        // Nav operations
        if(pause){
            checkPauseInput();
        }else if(!hasWon){            
            checkShiftInput();
        }else if(hasWon && Greenfoot.isKeyDown("enter")){
            removeObjects(getObjects(null));
            Greenfoot.setWorld(new TitleScreen());
        }

        if(Greenfoot.mouseClicked(backtomenu)) Greenfoot.setWorld(new TitleScreen());
    }

    public void checkShiftInput(){
        if((Greenfoot.isKeyDown("right") || Greenfoot.isKeyDown("left") || Greenfoot.isKeyDown("up") || Greenfoot.isKeyDown("down")) && !isDown){
            TitleScreen.cursor.play();
            if(Greenfoot.isKeyDown("right") && selectedColumn < wordLength-1){
                selectedColumn++;
                //System.out.println(Arrays.toString(selected));
                List<Actor> actors = getObjects(null);
                actors.removeAll(getObjects(Particle.class));
                removeObjects(actors);
                drawBoard();
            }else if(Greenfoot.isKeyDown("left") && selectedColumn > 0){
                selectedColumn--;
                //System.out.println(Arrays.toString(selected));
                List<Actor> actors = getObjects(null);
                actors.removeAll(getObjects(Particle.class));
                removeObjects(actors);
                drawBoard();
            }else if(Greenfoot.isKeyDown("up") && selected[selectedColumn] < board.get(selectedColumn).size()-1){
                selected[selectedColumn]++;
                String selectedStr = selectedString(board, selected);
                if(words.contains(selectedStr)){
                    if(!solvedwords.contains(selectedStr)){
                        TitleScreen.click.play();
                        counter.add();
                    }    
                    solvedwords.add(selectedStr);
                    for(int i = 0; i < selected.length; i++){
                        board.get(i).get(selected[i]).solved = true;
                        spawnParticles(offSet+blockSize*i, getHeight()/2);
                    }
                    List<Actor> actors = getObjects(null);
                    actors.removeAll(getObjects(Particle.class));
                    removeObjects(actors);
                    drawBoard();
                    for(int i = 0; i < selected.length; i++){
                        spawnParticles(offSet+blockSize*i, getHeight()/2);
                    }
                }else{
                    List<Actor> actors = getObjects(null);
                    actors.removeAll(getObjects(Particle.class));
                    removeObjects(actors);
                    drawBoard();
                }
                addObject(counter, 1080, 100);
                checkAchievements();
                // Send back to home screen after game completion
                if(checkBoard()){
                    boardCounter.add();
                    checkAchievements();
                    transition();
                }
            }else if(Greenfoot.isKeyDown("down") && selected[selectedColumn] > 0){
                selected[selectedColumn]--;
                String selectedStr = selectedString(board, selected);
                if(words.contains(selectedStr)){
                    if(!solvedwords.contains(selectedStr)){
                        TitleScreen.click.play();
                        counter.add();
                    }    
                    solvedwords.add(selectedStr);
                    for(int i = 0; i < selected.length; i++){
                        board.get(i).get(selected[i]).solved = true;
                        spawnParticles(offSet+blockSize*i, getHeight()/2);
                    }
                    List<Actor> actors = getObjects(null);
                    actors.removeAll(getObjects(Particle.class));
                    removeObjects(actors);
                    drawBoard();
                    for(int i = 0; i < selected.length; i++){
                        spawnParticles(offSet+blockSize*i, getHeight()/2);
                    }
                }else{
                    List<Actor> actors = getObjects(null);
                    actors.removeAll(getObjects(Particle.class));
                    removeObjects(actors);
                    drawBoard();
                }
                addObject(counter, 1080, 100);
                checkAchievements();
                // Send back to home screen after game completion
                if(checkBoard()){
                    boardCounter.add();
                    checkAchievements();
                    transition();
                }
            }
            isDown = true;
        }else if(!(Greenfoot.isKeyDown("right") || Greenfoot.isKeyDown("left") || Greenfoot.isKeyDown("up") || Greenfoot.isKeyDown("down")) && isDown){
            isDown = false;
        }
        if (Greenfoot.mouseClicked(backtomenu)){
            cursor.play();
            Greenfoot.setWorld(new TitleScreen());
        }
    }

    public void checkPauseInput(){
        if((Greenfoot.isKeyDown("right") || Greenfoot.isKeyDown("left") || Greenfoot.isKeyDown("up") || Greenfoot.isKeyDown("down") || Greenfoot.isKeyDown("enter")) && !isDown){
            TitleScreen.cursor.play();
            if(Greenfoot.isKeyDown("up") && pauseOption > 1){
                pauseOption--;
                drawPauseMenu();
            }else if(Greenfoot.isKeyDown("down") && pauseOption < 3){
                pauseOption++;
                drawPauseMenu();
            }
            if(Greenfoot.isKeyDown("enter")){
                switch(pauseOption){
                    case 1: // Play
                        pause = false;
                        removeObjects(getObjects(null));
                        drawBoard();
                        break;
                    case 2: // music option
                        if(TitleScreen.bgm.isPlaying()){
                            TitleScreen.bgm.pause();
                        }else{
                            TitleScreen.bgm.playLoop();
                        }
                        drawPauseMenu();
                        break;
                    case 3: // back to menu
                        Greenfoot.setWorld(new TitleScreen());
                        break;
                }
            }
            isDown = true;
        }else if(!(Greenfoot.isKeyDown("right") || Greenfoot.isKeyDown("left") || Greenfoot.isKeyDown("up") || Greenfoot.isKeyDown("down") || Greenfoot.isKeyDown("down")) && isDown){
            isDown = false;
        }
    }

    public ArrayList<ArrayList<Block>> createBoard(){
        // Create all new ArrayLists
        ArrayList<ArrayList<String>> boardTemp = new ArrayList<ArrayList<String>>();
        for(int i = 0; i < wordLength; i++){
            ArrayList<String> temp = new ArrayList<String>();
            boardTemp.add(temp);
        }
        // Put the words into the board object
        for(int i = 0; i < numOfWords; i++){
            // Get a word and split
            String strT = words.get(r.nextInt(words.size()));
            String[] strArr = strT.split("");
            // If the section doesn't already have said letter, add it
            for(int j = 0; j < wordLength; j++){
                if(!boardTemp.get(j).contains(strArr[j])){
                    boardTemp.get(j).add(strArr[j]);
                }
            }
        }
        // Copy over into a 2D Block Array for efficient completion check
        ArrayList<ArrayList<Block>> board2 = new ArrayList<ArrayList<Block>>();
        for(int i = 0; i < wordLength; i++){
            ArrayList<Block> temp = new ArrayList<Block>();
            board2.add(temp);
        }
        for(int i = 0; i < wordLength; i++){
            for(int j = 0; j < boardTemp.get(i).size(); j++){
                Block temp = new Block();
                temp.solved = false;
                temp.item = boardTemp.get(i).get(j);
                board2.get(i).add(temp);
            }
        }

        for(ArrayList<Block> s : board2){
            Collections.shuffle(s);
        }

        return board2;
    }

    // Draw the board
    public void drawBoard(){
        boolean toggle = false;
        for(int i = 0; i < selected.length; i++){
            int x = offSet+(blockSize*i);
            String prefix1 = "S" + (board.get(i).get(selected[i]).solved ? "S" : "U");

            int px = offSet+blockSize*i;
            addObject(new Button(new GreenfootImage(prefix1 + "_" + (board.get(i).get(selected[i]).item.toUpperCase()) + ".png"), blockSize, 1), px, getHeight()/2);
            for(int j = selected[i]-1; j > -1; j--){
                String prefix = "U" + (board.get(i).get(j).solved ? "S" : "U");
                addObject(new Button(new GreenfootImage(prefix + "_" + (board.get(i).get(j).item.toUpperCase()) + ".png"), blockSize, 1), px, getHeight()/2 - blockSize*(selected[i]-j));
            }
            if(i == selectedColumn){
                addObject(new Button(new GreenfootImage("1.png"), blockSize, 1), px, getHeight()/2 - (blockSize*(selected[i]+1)));
            }
            for(int j = selected[i]+1; j < board.get(i).size(); j++){
                String prefix = "U" + (board.get(i).get(j).solved ? "S" : "U");
                addObject(new Button(new GreenfootImage(prefix + "_" + (board.get(i).get(j).item.toUpperCase()) + ".png"), blockSize, 1), px, getHeight()/2 + blockSize*(j-selected[i]));
            }
            if(i == selectedColumn){
                addObject(new Button(new GreenfootImage("2.png"), blockSize, 1), px, Math.abs(getHeight()/2 + blockSize*(board.get(i).size() - selected[i])));
            }
        }

    } 

    // Draw the paused menu
    public void drawPauseMenu(){
        removeObjects(getObjects(null));
        addObject(new Button(new GreenfootImage("Pause Menu Background.png"), getHeight(), 1), getWidth()/2, getHeight()/2);
        addObject(new Button(new GreenfootImage("PMenuResume" + ((pauseOption == 1) ? "-2" : "-1") + ".png"), getHeight()/10, 3.8), getWidth()/2, getHeight()*3/6);
        addObject(new Button(new GreenfootImage("PMenuSound" + ((TitleScreen.bgm.isPlaying()) ? "On" : "Off") + ((pauseOption == 2) ? "-2" : "-1") + ".png"), getHeight()/10, 3.8), getWidth()/2, getHeight()*4/6);
        addObject(new Button(new GreenfootImage("PMenuBackToMenu" + ((pauseOption == 3) ? "-2" : "-1") + ".png"), getHeight()/10, 3.8), getWidth()/2, getHeight()*5/6);
    }

    // Get the selected String
    public String selectedString(ArrayList<ArrayList<Block>> board, int[] selected){
        String selectedString = "";
        for(int i = 0; i < selected.length; i++){
            selectedString += board.get(i).get(selected[i]).item;
        }
        return selectedString;
    }

    // Check if String is a number
    public static boolean isNumeric(String str) { 
        try {  
            Double.parseDouble(str);  
            return true;
        } catch(NumberFormatException e){  
            return false;
        }
    }

    // Check if the current board is fully "solved"
    public boolean checkBoard(){
        for(ArrayList<Block> arr : board){
            for(Block b : arr){
                if(!b.solved){
                    return false;
                }
            }
        }
        return true;
    }

    private void checkAchievements()
    {
        int w = counter.getScore();
        int x = boardCounter.getScore();

        if(!wordCheck.contains(w) && wordAchievements.containsKey(w)){
                Slide s = new Slide(wordAchievements.get(w));
                addObject(s, 640, 60);
                Greenfoot.delay(50);
        }
        wordCheck.add(w);
        if(!boardcheck.contains(x) && boardAchievements.containsKey(x)){
                Slide s = new Slide(boardAchievements.get(x));
                addObject(s, 640, 60);
                Greenfoot.delay(50);
        }
        boardcheck.add(x);
        
    }

    public void spawnParticles(int x, int y){
        for(int j = 0; j <= NUM_OF_PARTICLES; j++){
            addObject(new Particle(), x, y);
        }
    }

    public void transition(){
        hasWon = true;
        GreenfootImage img = new GreenfootImage("Win Screen.png");
        Picture p = new Picture(img);
        addObject(p,getWidth()/2+50, getHeight()/2+15);
        addObject(backtomenu,950,525);
    }
}
