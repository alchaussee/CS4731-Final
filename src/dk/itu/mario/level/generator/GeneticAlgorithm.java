/*
 * @author Richard Chaussee and Clayton Pierce
 * This is a genetic algorithm to generate Mario levels based off of player models.
 */

package dk.itu.mario.level.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import dk.itu.mario.MarioInterface.GamePlay;
import dk.itu.mario.engine.sprites.SpriteTemplate;
import dk.itu.mario.level.Level;
import dk.itu.mario.level.MyLevel;

public class GeneticAlgorithm 
{
	private static final int NORMAL_POPULATION = 10;
	private static final int MAX_POPULATION = 50;
	private static final double THRESHOLD = 0.00005;
	private static final double PROB_CROSSOVER = 0.90;
	private static final double PROB_MUTATE = 0.60;
	private static final int MAX_ITERATIONS = 10000;
	public int interationCount = 0;
	private double prevError = Double.MAX_VALUE;
	public double deltaError = THRESHOLD;
	
    private static final byte GROUND = (byte) (1 + 9 * 16);
	private static final byte COIN = (byte) (2 + 2 * 16);
	private static final byte HILL_TOP_LEFT = (byte) (4 + 8 * 16);
	private static final byte CANNON_TOP = (byte) (14 + 0 * 16);
	private static final byte TUBE_TOP_LEFT = (byte) (10 + 0 * 16);
	private static final byte LEFT_GRASS_EDGE = (byte) (0+9*16);
	private static final byte HILL_FILL = (byte) (5 + 9 * 16);
    private static final byte HILL_LEFT = (byte) (4 + 9 * 16);
    private static final byte HILL_RIGHT = (byte) (6 + 9 * 16);
    private static final byte HILL_TOP = (byte) (5 + 8 * 16);
    private static final byte HILL_TOP_RIGHT = (byte) (6 + 8 * 16);
    private static final byte HILL_TOP_LEFT_IN = (byte) (4 + 11 * 16);
    private static final byte HILL_TOP_RIGHT_IN = (byte) (6 + 11 * 16);
    private static final byte TUBE_TOP_RIGHT = (byte) (11 + 0 * 16);
    private static final byte TUBE_SIDE_LEFT = (byte) (10 + 1 * 16);
    private static final byte TUBE_SIDE_RIGHT = (byte) (11 + 1 * 16);
    private static final byte RIGHT_GRASS_EDGE = (byte) (2+9*16);
    private static final byte GRASS = (byte) (1+8*16);
    private static final byte RIGHT_UP_GRASS_EDGE = (byte) (2+8*16);
    private static final byte LEFT_UP_GRASS_EDGE = (byte) (0+8*16);
    private static final byte LEFT_POCKET_GRASS = (byte) (3+9*16);
    private static final byte RIGHT_POCKET_GRASS = (byte) (3+8*16);
    private static final byte BLUE_GOAL_TOP = (byte) (12+4*16);
    private static final byte BLUE_GOAL = (byte) (12+5*16);
    private static final byte PURPLE_GOAL_TOP = (byte) (13+4*16);
    private static final byte PURPLE_GOAL = (byte) (13+5*16);
    private static final byte GOAL_BAR = (byte) (13+3*16);
    private static final byte GOAL_BAR_END = (byte) (12+3*16);
	
	private ArrayList<MyLevel> population = new ArrayList<MyLevel>();
	private GamePlay model;
	private Random globalRand;
	
	MyLevel bestLevel = null;
	
	public GeneticAlgorithm(int width, int height, int type, GamePlay playerMetrics)
	{
		this.model = playerMetrics;
		Random rand = new Random();
		globalRand = new Random();
		//Create initial population
		for(int i=0;i<NORMAL_POPULATION;i++)
		{
			MyLevel level = new MyLevel(width, height, rand.nextLong(), 0, type, playerMetrics);
			level.fitness = Fitness(level);
			population.add(level);
			bestLevel = level;
		}
	}
	
	public void run()
	{
		this.interationCount++;
		double error = 0;
		Random rand = new Random();
		population.sort(new LevelComparator());
		
		while(population.size()<MAX_POPULATION)
		{
			MyLevel level1 = weightedPick(population);
			MyLevel level2 = weightedPick(population);
			
			MyLevel offspring = null;
			if(rand.nextDouble()<=PROB_CROSSOVER)
			{
				offspring = crossover(level1, level2);
			}
			else
			{
				if(rand.nextDouble()<=0.5)
					offspring = level1;
				else
					offspring = level2;
			}
			
			if(rand.nextDouble()<=PROB_MUTATE)
			{
				offspring = mutate(offspring);
			}
			
			offspring.fitness = Fitness(offspring);
			population.add(offspring);
		}
		
		for(int i=0;i<population.size();i++)
			error+=population.get(i).fitness;
		
		this.deltaError = Math.abs(error-this.prevError);
		this.prevError = error;
		population.sort(new LevelComparator());
		
		for(int i=NORMAL_POPULATION;i<population.size()-NORMAL_POPULATION;i++)
			population.remove(i);
		
		if(population.get(0).fitness>bestLevel.fitness)
			bestLevel = population.get(0).cloneMyLevel();
		bestLevel.fitness = Fitness(bestLevel);
	}
	
	public boolean isDone()
	{
		return this.interationCount>MAX_ITERATIONS;
	}
	
	public Level getLevel()
	{
		int[] array = countElements(bestLevel);
		System.out.println("Fitness rating: " + bestLevel.fitness);
		System.out.println("Number of coins: " + array[0]);
		System.out.println("Number of jumps: " + array[1]);
		System.out.println("Number of goombas: " + array[2]);
		return bestLevel;
	}
	
	public double Fitness(MyLevel level)
	{
		int[] counts = countElements(level);
		int numCoins = counts[0];
		int numJumps = counts[1];
		int numGoomba = counts[2];
		
		double coinFit = numCoins*1.0/(this.model.coinsCollected);
		double jumpFit = numJumps*1.0/(this.model.jumpsNumber);
		double goombaFit = numGoomba*1.0/(this.model.GoombasKilled);
		
		// Fitness adjustments.
		if(Double.isNaN(coinFit))
			coinFit = 0.0;
		if(Double.isNaN(goombaFit))
			goombaFit = 0.0;
		if(Double.isNaN(jumpFit))
			jumpFit = 0.0;

		if(coinFit>2)
			coinFit = -1;
		else if(coinFit>1.5)
			coinFit = 1/(coinFit*2);
		else if(coinFit>1.0)
			coinFit = 1.0;
		
		if(jumpFit>2)
			jumpFit = -1;
		else if(jumpFit>1.5)
			jumpFit = 1/(jumpFit*2);
		else if(jumpFit>1.0)
			jumpFit = 1.0;

		if(goombaFit>3)
			goombaFit = -1;
		else if(goombaFit>2)
			goombaFit = 1/(goombaFit*2);
		else if(goombaFit>1.0)
			goombaFit = 1.0;
		
		double fitness = (coinFit+jumpFit+goombaFit)/3.0;
		if(Double.isNaN(fitness) || Double.isInfinite(fitness))
			return 0.0;
		return fitness;
	}
	
	private MyLevel crossover(MyLevel l1, MyLevel l2)
	{
		MyLevel ret = l1;
		Random rand = new Random();
		int split = rand.nextInt(l1.getWidth());
		boolean redo = false;
		do
		{
			redo = false;
			split = rand.nextInt(l1.getWidth());
			
			for(int i=0;i<l1.getHeight();i++)
			{
				byte b = l1.getBlock(split, i);
				if(b == HILL_FILL || b == HILL_LEFT || b == HILL_RIGHT || 
						b == TUBE_SIDE_LEFT || b == TUBE_SIDE_RIGHT)
				{
					redo = true;
					break;
				}
				
				b = l2.getBlock(split, i);
				if(b == HILL_FILL || b == HILL_LEFT || b == HILL_RIGHT || 
						b == TUBE_SIDE_LEFT || b == TUBE_SIDE_RIGHT)
				{
					redo = true;
					break;
				}
			}
		} while(redo);
		
		for(int i=split;i<l1.getWidth();i++)
		{
			for(int j=0;j<l1.getHeight();j++)
			{
				byte block = l2.getBlock(i, j);
				
				if(block == GOAL_BAR || block == GOAL_BAR_END || block == PURPLE_GOAL || block == PURPLE_GOAL_TOP || block == BLUE_GOAL || block == BLUE_GOAL_TOP)
				{
					ret.setBlock(i, j, (byte)0);
					continue;
				}
				
				if(i > 0 && i < l1.getWidth()-1)
					if(l2.getBlock(i+1, j)==0 && l1.getBlock(i-1, j)==0)
						ret.setBlock(i, j, (byte)0);
					else if((block == GROUND || block == LEFT_POCKET_GRASS || block == RIGHT_POCKET_GRASS) && l2.getBlock(i, j-1)==0)
						ret.setBlock(i, j, GRASS);
					else if(l2.getBlock(i+1, j) == 0)
						if(block == GROUND)
							ret.setBlock(i, j, RIGHT_GRASS_EDGE);
						else if(block == GRASS)
							ret.setBlock(i, j, RIGHT_UP_GRASS_EDGE);
						else
							ret.setBlock(i, j, block);
					else if(l1.getBlock(i-1, j) == 0)
						if(block == GROUND)
							ret.setBlock(i, j, LEFT_GRASS_EDGE);
						else if(block == GRASS)
							ret.setBlock(i, j, LEFT_UP_GRASS_EDGE);
						else
							ret.setBlock(i, j, block);
					else
						ret.setBlock(i, j, block);
				
				ret.setSpriteTemplate(i, j, l2.getSpriteTemplate(i, j));
			}
		}
		return ret;
	}
	
	private MyLevel mutate(MyLevel offspring)
	{
		Random rand = new Random();
		int choice = rand.nextInt(8);
		switch(choice)
		{
			//Add coin
			case(0):
			{
				int x = rand.nextInt(offspring.getWidth());
				int y = rand.nextInt(offspring.getHeight());
				while(offspring.getBlock(x, y)!=0)
				{
					x = rand.nextInt(offspring.getWidth());
					y = rand.nextInt(offspring.getHeight());
				}
				y = findReasonableHeight(offspring, x, y, 5);
				offspring.setBlock(x, y, COIN);
				break;
			}
			//Remove coin
			case(1):
			{
				removeRandomCoin(offspring);
				break;
			}
			//Add Goomba
			case(2):
			{
				int x = rand.nextInt(offspring.getWidth());
				int y = rand.nextInt(offspring.getHeight());
				if(offspring.getBlock(x, y)==0)
				{	
					y = findReasonableHeight(offspring, x, y, 3);
					offspring.setSpriteTemplate(x, y, new SpriteTemplate(SpriteTemplate.GOOMPA, false));
				}
				break;
			}
			//Remove Goomba
			case(3):
			{
				removeRandomGoomba(offspring);
				break;
			}
			//Add Hills
			case(4):
			{
				// Take a look at myLevel.java build hill straight private function
				int chance = rand.nextInt(20);
				if(chance == 0)
				{
					int x = rand.nextInt(offspring.getWidth());
					offspring = buildHillStraight(x, 20, offspring, 0);
				}
				break;
			}
			//Remove Hills
			case(5):
			{
				// Remember, sometimes you may not every have a hill
				//offspring = removeHill(offspring);
				break;
			}
			//Add Pipes
			case(6):
			{
				boolean test = false;
				int count = 0;
				do
				{
					int x = rand.nextInt(offspring.getWidth());
					int y = rand.nextInt(offspring.getHeight());
					if(offspring.getBlock(x, y) == GRASS)
						test = buildPipe(x, y-1, offspring);
					count++;
					if(count > 100) break;
				} while(!test);
				break;
			}
			//Remove Pipes
			case(7):
			{
				removeRandomPipe(offspring);
				break;
			}
			default:
				break;
		}
				
		return offspring;
	}
	
	private int findReasonableHeight(MyLevel level, int x, int y, int range)
	{
		Random rand = new Random();
		int yy = y;
		int h = level.getHeight();
		while(level.getBlock(x, yy) == 0 && yy++ < h);
		return ((yy-1) - rand.nextInt(range));
	}
	
	private MyLevel buildHillStraight(int xo, int maxLength, MyLevel level, int count)
    {
		if(count > 100)
			return level;
		Random random = new Random();
        int length = random.nextInt(10) + 10;
        if (length > maxLength) length = maxLength;
        int grassHeight = 0;
        for(int y = 0; y < level.getHeight(); y++)
        {
        	if(level.getBlock(xo, y) == GRASS)
        	{
        		grassHeight = y;
        		break;
        	}
        }
        for(int x = xo; x < length+xo; x++)
        {
        	if(level.getBlock(x,grassHeight) != GRASS)
        	{
        		int newX = random.nextInt(level.getWidth());
        		return buildHillStraight(newX, maxLength, level, ++count);
        	}
        }
        
        int h = grassHeight;

        boolean keepGoing = true;

        boolean[] occupied = new boolean[length];
        while (keepGoing)
        {
            h = h - 2 - random.nextInt(3);

            if (h <= 0)
            {
                keepGoing = false;
            }
            else
            {
                int l = random.nextInt(5) + 3;
                int xxo = random.nextInt(length - l - 2) + xo + 1;

                if (occupied[xxo - xo] || occupied[xxo - xo + l] || occupied[xxo - xo - 1] || occupied[xxo - xo + l + 1])
                {
                    keepGoing = false;
                }
                else
                {
                    occupied[xxo - xo] = true;
                    occupied[xxo - xo + l] = true;
                    if (random.nextInt(4) == 0)
                    {
                        keepGoing = false;
                    }
                    for (int x = xxo; x < xxo + l; x++)
                    {
                        for (int y = h; y < grassHeight; y++)
                        {
                            int xx = 5;
                            if (x == xxo) xx = 4;
                            if (x == xxo + l - 1) xx = 6;
                            int yy = 9;
                            if (y == h) yy = 8;

                            if (level.getBlock(x, y) == 0)
                            {
                                level.setBlock(x, y, (byte) (xx + yy * 16));
                            }
                            else
                            {
                                if (level.getBlock(x, y) == HILL_TOP_LEFT) level.setBlock(x, y, HILL_TOP_LEFT_IN);
                                if (level.getBlock(x, y) == HILL_TOP_RIGHT) level.setBlock(x, y, HILL_TOP_RIGHT_IN);
                            }
                        }
                    }
                }
            }
        }
        
       return level; 
    }

	private MyLevel removeHill(MyLevel level)
	{
		// Give a number of tries to find a HILL_TOP_LEFT
		int tries = 1000;
		boolean found = false;
		int x = 0;
		int y = 0;
		while(tries > 0 && !found)
		{
			x = globalRand.nextInt(level.getWidth());
			y = globalRand.nextInt(level.getHeight());
			if(level.getBlock(x, y) == HILL_TOP_LEFT)
			{
				found = true;
			}
			tries--;
		}
		if(tries <= 0)
		{
			return level;
		}
		int width = 1;
		int maxWidth = 15;
		while(width < maxWidth)
		{
			if(level.getBlock(x+width+1, y) == (byte)0)
			{
				width++;
				break;
			}
			else
			{
				width++;
			}
		}
		if(width >= maxWidth)
		{
			return level;
		}
		int height = 1;
		int maxHeight = 15;
		while(height < maxHeight)
		{
			if(level.getBlock(x, y+height+1) == GRASS ||
					level.getBlock(x, y+height+1) == HILL_TOP_LEFT ||
					level.getBlock(x, y+height+1) == HILL_TOP_RIGHT ||
					level.getBlock(x, y+height+1) == HILL_TOP)
			{
				height++;
				break;
			}
			else
			{
				height++;
			}
		}
		if(height >= maxHeight)
		{
			return level;
		}
		
		// Now that we have the width and height, set the blocks to air
		for(int i = 0; i < width; i++)
		{
			for(int j = 0; j < height; j++)
			{
				level.setBlock(x+i, y+j, (byte)0);
			}
		}
		return level;
	}
	
	private MyLevel weightedPick(ArrayList<MyLevel> population)
	{
		Random rand = new Random();
		
		double t = rand.nextDouble();
		for(int i=49;i>=0;i--)
		{
			if(t <= 1.0/(Math.pow(2, i)))
			{
				return population.get(i);
			}
		}
		return population.get(0);
	}
	
	private static int[] countElements(Level level)
	{
		//Coins Jumps Goombas
		int[] ret = {0,0,0};
		byte[][] map = level.getMap();
		SpriteTemplate[][] sprites = level.getSpriteTemplate();
		
		for(int x=0;x<level.getWidth();x++)
		{
			for(int y=0;y<level.getHeight();y++)
			{
				if(map[x][y] == COIN)
					ret[0]++;
				if(map[x][y] == HILL_TOP_LEFT || map[x][y] == CANNON_TOP || map[x][y] == LEFT_GRASS_EDGE 
						|| map[x][y] == TUBE_TOP_LEFT)
					ret[1]++;
				if(sprites[x][y]!=null && sprites[x][y].type==SpriteTemplate.GOOMPA)
				{
					ret[2]++;
					ret[1]++; //Have to jump over goombas
				}
			}
		}
		return ret;
	}
	
	private static void removeRandomCoin(Level level)
	{
		ArrayList<int[]> coinsLocations = new ArrayList<int[]>();
		byte[][] map = level.getMap();
		
		for(int x=0;x<level.getWidth();x++)
		{
			for(int y=0;y<level.getHeight();y++)
			{
				if(map[x][y] == COIN)
				{
					int[] t = {x,y};
					coinsLocations.add(t);
				}
			}
		}
		
		Random rand = new Random();
		if(coinsLocations.isEmpty())
			return;
		int[] loc = coinsLocations.get(rand.nextInt(coinsLocations.size()));
		level.setBlock(loc[0], loc[1], (byte)0);
	}
	
	private static void removeRandomGoomba(Level level)
	{
		ArrayList<int[]> goombaLocations = new ArrayList<int[]>();
		SpriteTemplate[][] sprites = level.getSpriteTemplate();
		
		for(int x=0;x<level.getWidth();x++)
		{
			for(int y=0;y<level.getHeight();y++)
			{
				if(sprites[x][y] != null && sprites[x][y].type == SpriteTemplate.GOOMPA)
				{
					int[] t = {x,y};
					goombaLocations.add(t);
				}
			}
		}
		
		Random rand = new Random();
		if(goombaLocations.isEmpty())
			return;
		int[] loc = goombaLocations.get(rand.nextInt(goombaLocations.size()));
		level.setSpriteTemplate(loc[0], loc[1], null);
	}
	
	private boolean buildPipe(int x, int y, MyLevel level)
    {
		Random rand = new Random();
		int pipeHeight = 2 + rand.nextInt(3);
		for(int i=0;i<pipeHeight;i++)
		{
			for(int j=0;j<2;j++)
			{
				if(level.getBlock(x+j, y-i) != (byte)0)
					return false;
			}
		}
		
		for(int i=0;i<pipeHeight-1;i++)
		{
			level.setBlock(x, y-i, TUBE_SIDE_LEFT);
			level.setBlock(x+1, y-i, TUBE_SIDE_RIGHT);
		}
		level.setBlock(x, y-pipeHeight+1, TUBE_TOP_LEFT);
		level.setBlock(x+1, y-pipeHeight+1, TUBE_TOP_RIGHT);
		
        return true;
    }
	
	private boolean removeRandomPipe(MyLevel level)
	{
		Random rand = new Random();
		ArrayList<int[]> pipeLocations = new ArrayList<int[]>();
		for(int x=0;x<level.getWidth();x++)
		{
			for(int y=0;y<level.getHeight();y++)
			{
				if(level.getBlock(x, y) == TUBE_TOP_LEFT)
				{
					int[] t = {x,y};
					pipeLocations.add(t);
				}
			}
		}
		if(pipeLocations.size() == 0)
			return false;
		int ind = rand.nextInt(pipeLocations.size());
		int x = pipeLocations.get(ind)[0];
		int y = pipeLocations.get(ind)[1];
		
		for(int i=0;i<6;i++)
		{
			for(int j=0;j<2;j++)
			{
				if(level.getBlock(x+j, y+i)==GRASS || level.getBlock(x+j, y+i)==GROUND)
					continue;
				level.setBlock(x+j, y+i, (byte)0);
			}
		}
		return true;
	}

}

class LevelComparator implements Comparator<MyLevel>
{
	public int compare(MyLevel arg0, MyLevel arg1)
	{
		double f = arg0.fitness-arg1.fitness;
		if(f>0)
			return -1;
		else if(f<0)
			return 1;
		else
			return 0;
	}
}


