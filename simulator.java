import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class simulator {
    cache l1_cache;
    cache l2_cache;
    int l2size;
    Scanner trace_file = null;
    int i;

    // Constructor
    public simulator(cache l1, cache l2, String trace, int l2_size) {
        l1_cache = l1;
        l2_cache = l2;
        l2size = l2_size;
        File file = new File(trace);
        try {
            trace_file = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //binary to hexa decimal converter
    public String b2h(String bin){
        while (bin.length() < 32) {
            bin += "0";
        }
        int decimal_value = Integer.parseInt(bin,2);
        String hexa = Integer.toString(decimal_value,16);
        return hexa;
    }


    // Run Cache Simulator
    public void begin(int inclusion) {
        while (trace_file.hasNextLine()) {
            i++;
            String[] address = trace_file.nextLine().split(" ");
            String op = address[0];
            String addr = address[1];
            boolean l1_check_status = false;
            boolean l2_status;
            String out;
            String ad;

            if (op.equals("w")) {
                l1_check_status = l1_cache.ReadOrWrite(addr,false);
            }
            else if (op.equals("r")) {
                l1_check_status = l1_cache.ReadOrWrite(addr,true);
            }
            // L1 miss
            if (l1_check_status == false && l2size == 0) {
                l1_cache.Alloc(addr, i, op);
            }
            else if (l1_check_status == false && l2size != 0) {
                out = l1_cache.Alloc(addr, i, op);
                //L1 to L2 write back
                if (! out.equals("")) {
                    ad = b2h(out);
                    l2_status = l2_cache.ReadOrWrite(addr,false);
                    // L2 write miss
                    if (l2_status == false){
                        l2_cache.Alloc(ad, i, "w");
                    }
                }
                l2_status = l2_cache.ReadOrWrite(addr,true);
                // L2 read miss
                if (l2_status == false){
                    out = l2_cache.Alloc(addr, i, op);
                    // For Inclusion cache
                    if (! out.equals("") && inclusion == 1) {
                        ad = b2h(out);
                        l1_cache.make_Invalid(ad);
                    }
                }
            }
        }
    }

    // Print final results
    public void print_results() {
        System.out.println("====== L1 contents ======");
        print_content(l1_cache);
        if (l2size !=0) {
            System.out.println("====== L2 contents ======");
            print_content(l2_cache);
        }
        System.out.println("===== Simulation results (raw) =====");
        System.out.println("a. number of L1 reads        : " + l1_cache.read);
        System.out.println("b. number of L1 read misses  : " + l1_cache.read_miss);
        System.out.println("c. number of L1 writes       : " + l1_cache.write);
        System.out.println("d. number of L1 write misses : " + l1_cache.write_miss);
        float l1_miss_rate = (float) (l1_cache.read_miss + l1_cache.write_miss)/
                (float) (l1_cache.read + l1_cache.write);
        System.out.println("e. L1 miss rate              : " + l1_miss_rate);
        System.out.println("f. number of L1 writebacks   : " + l1_cache.write_back);
        int traffic = l1_cache.read_miss + l1_cache.write_miss + l1_cache.write_back;
        if (l2size != 0) {
            System.out.println("g. number of L2 reads        : " + l2_cache.read);
            System.out.println("h. number of L2 read misses  : " + l2_cache.read_miss);
            System.out.println("i. number of L2 writes       : " + l2_cache.write);
            System.out.println("j. number of L2 write misses : " + l2_cache.write_miss);
            float l2_miss_rate = (float) (l2_cache.read_miss)/ (float) (l2_cache.read);
            System.out.println("k. L2 miss rate              : " + l2_miss_rate);
            System.out.println("l. number of L2 writebacks   : " + l2_cache.write_back);
            traffic = l2_cache.read_miss + l2_cache.write_miss + l2_cache.write_back + l1_cache.write_back_m;
        }
        else {
            System.out.println("g. number of L2 reads        : " + 0);
            System.out.println("h. number of L2 read misses  : " + 0);
            System.out.println("i. number of L2 writes       : " + 0);
            System.out.println("j. number of L2 write misses : " + 0);
            System.out.println("k. L2 miss rate              : " + 0);
            System.out.println("l. number of L2 writebacks   : " + 0);
        }
        System.out.println("m. total memory traffic      : " + traffic);
    }

    public void print_content(cache cc) {
        for (int i=0; i<cc.sets; i++) {
            System.out.print("Set\t"+i+" : ");
            for (int j=0; j< cc.asc; j++) {
                if (cc.valid[i][j] == 1) {
                    int dec = Integer.parseInt(cc.cache[i][j],2);
                    String hex = Integer.toString(dec,16);
                    System.out.print(hex + " ");
                    if (cc.drty_ind[i][j] == 1) {
                        System.out.print("D\t");
                    }else {
                        System.out.print(" \t");
                    }
                }
            }
            System.out.print("\n");
        }
    }
}


