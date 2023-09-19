//  cache.java
//  Cache
//
//  Created on 10/01/22.
//
//  The Below code contains implentations of Cache 
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class cache {
    String[][] cache;
    int sets;
    int asc;
    int tag;
    int id; // index
    int off; // offset
    int[][] lru;
    int[][] drty_ind;
    int[][] valid;
    ArrayList<String> opt;
    String[][] tag_index;
    int read = 0;
    int read_miss = 0;
    int write = 0;
    int write_miss = 0;
    int write_back = 0;
    int write_back_m = 0;
    int rep_policy;
    int count;

    // Cache Constructor
    public cache (int blck_size, int cache_size, int assoc, int replacement_policy, String trace) {
        if (cache_size != 0) {
            sets = cache_size / (blck_size * assoc);
            asc = assoc;
            rep_policy = replacement_policy;
            cache = new String[sets][assoc];
            tag_index = new String[sets][assoc];
            drty_ind = new int[sets][assoc];
            valid = new int[sets][assoc];
            off = (int) (Math.log(blck_size) / Math.log(2));
            id = (int) (Math.log(sets) / Math.log(2));
            tag = 32 - id - off;
            if (rep_policy == 0 || asc == 1){
                lru = new int[sets][asc];
            }else if (rep_policy == 1) {
                int x = 0;
                int p = 1;
                float value_temp = 0.1f;
                while (value_temp != 0) {
                    value_temp = (float) (asc / Math.pow(2, p));
                    if (value_temp > 0.5) {
                        x += Math.ceil(value_temp);
                        p += 1;
                    } else {
                        value_temp = 0;
                    }
                }
                lru = new int[sets][x];
            }else if (rep_policy == 2){
                opt = new ArrayList<>();
                Scanner trace_copy = null;
                File file_copy = new File(trace);
                try {
                    trace_copy = new Scanner(file_copy);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                while (trace_copy.hasNextLine()) {
                    String[] address = trace_copy.nextLine().split(" ");
                    String addr = address[1];
                    int v = Integer.parseInt(addr, 16);
                    String v1 = Integer.toBinaryString(v);
                    while (v1.length() < 32) {
                        v1 = "0" + v1;
                    }
                    opt.add(v1.substring(0,(tag+id)));
                }
            }
        }
    }

    // Get tag and index from address
    public List tag_index(String addr) {
        List str_ti = new ArrayList();
        int value = Integer.parseInt(addr, 16);
        String value1 = Integer.toBinaryString(value);
        while (value1.length() < 32) {
            value1 = "0" + value1;
        }
        String str_tg = value1.substring(0, tag);
        String ix = value1.substring(tag, tag+id);
        str_ti.add(str_tg);
        str_ti.add(ix);
        return str_ti;
    }

    // Method to Read or Write
    public boolean ReadOrWrite(String addr, boolean isRead){
        boolean hit     = false;
        int ix         = 0;
        List ix_tag    = tag_index(addr);
        String str_tg       = (String) ix_tag.get(0);
        String ix1     = (String) ix_tag.get(1);
        if (isRead){
          read++;
        } else{
          write++;
        }
        if (sets != 1) {
          ix = Integer.parseInt(ix1, 2);
        }
  
        for (int i = 0; i < asc ; i++) {
          if (valid[ix][i] == 1){
            if (cache[ix][i].equals(str_tg)) {
              if (rep_policy == 0 || asc == 1) {
                u_LRU(ix, i);
              } else if (rep_policy == 1) {
                u_pLRU(ix, i);
              }
              if (!isRead){ 
                drty_ind[ix][i] = 1; 
                }
              hit = true;
              break;
            }
          }
        }
        if (hit == false) {
          if (isRead){
            read_miss++;
          } else{
            write_miss++;
          }
        }
        return hit;
      }

    // When cache miss tries to allocate address into empty block or calls replacement policy
    public String Alloc(String addr, int count, String op){
        boolean status = false;
        int id = 0;
        List t = tag_index(addr);
        String tag = (String) t.get(0);
        String idx1 = (String) t.get(1);
        if (sets != 1) {
            id = Integer.parseInt(idx1, 2);
        }

        for (int i = 0; i < asc ; i++) {
            if (valid[id][i] == 0) {
                cache[id][i] = tag;
                tag_index[id][i] = tag+idx1;
                valid[id][i] = 1;
                status = true;
                if (rep_policy == 0 || asc == 1) {
                    u_LRU(id, i);
                }
                else if (rep_policy == 1) {
                    u_pLRU(id, i);
                }
                if (op.equals("w")) {
                    drty_ind[id][i] = 1;
                }
                else {
                    drty_ind[id][i] = 0;
                }
                break;
            }
        }
        if (status == false) {
            return Replace_alloc(addr, count, op);
        }
        return "";
    }

    // Replace cache block using replacement policy
    public String Replace_alloc(String addr, int counter, String op) {
        int idx = 0;
        List ti = tag_index(addr);
        String tg = (String) ti.get(0);
        String idxx = (String) ti.get(1);
        if (sets != 1) {
            idx = Integer.parseInt(idxx, 2);
        }
        String out = "";

        int r_ix = 0;
        if (rep_policy == 0 || asc == 1) {
            r_ix = LRU(idx);
        }
        else if (rep_policy == 1) {
            r_ix = PLRU(idx);
        }
        else if (rep_policy == 2){
            r_ix = OPT(idx, counter);
        }

        if (drty_ind[idx][r_ix] == 1){
            write_back++;
            out = tag_index[idx][r_ix];
        }
        cache[idx][r_ix] = tg;
        tag_index[idx][r_ix] = tg+idxx;
        valid[idx][r_ix] = 1;

        if (rep_policy == 0 || asc == 1) {
            u_LRU(idx, r_ix);
        }
        else if (rep_policy == 1) {
            u_pLRU(idx, r_ix);
        }

        if (op.equals("w")) {
            drty_ind[idx][r_ix] = 1;
        }else {
            drty_ind[idx][r_ix] = 0;
        }
        return out;
    }

    // Make invalid
    public void make_Invalid(String addr){
        int out = 0;
        int idx = 0;
        List ti = tag_index(addr);
        String tg = (String) ti.get(0);
        String idxx = (String) ti.get(1);
        if (sets != 1) {
            idx = Integer.parseInt(idxx, 2);
        }

        for (int i=0; i<asc; i++) {
            if (cache[idx][i].equals(tg)) {
                    valid[idx][i] = 0;
                    out = drty_ind[idx][i];
                    break;
            }
        }
        if (out == 1) {
            write_back_m++;
        }
    }

    // get LRU block
    public int LRU(int set) {
        int min_idx = 0;
        for (int i=0 ; i < asc ; i++) {
            if (lru[set][i] < lru[set][min_idx]) {
                min_idx = i;
            }
        }
        return min_idx;
    }

    public void u_LRU(int set, int asc) {
        lru[set][asc] = count;
        count++;
    }

    // Pseudo - LRU block
    public int PLRU(int set) {
        String x = "";
        int i = 1;
        while(i-1 < lru[0].length) {
            x += lru[set][i-1];
            if (lru[set][i-1] == 0) {
                i = (2*i) + 1;
            } else {
                i = 2*i;
            }
        }
        String[] xx = x.split("");
        String y = "";
        for (int j=0; j<x.length(); j++) {
            if (xx[j].equals("0")){
                y += "1";
            }
            else {
                y += "0";
            }
        }
        return Integer.parseInt(y,2);
    }

    public void u_pLRU(int set, int ix) {
        String str = Integer.toBinaryString(ix);
        int l = (int) Math.ceil( Math.log(asc) / Math.log(2) );
        while (str.length() < l) {
            str = "0" + str;
        }
        String[] str1 = str.split("");
        int k = 1;
        for (int i=0; i<str.length(); i++) {
            lru[set][k-1] = Integer.parseInt(str1[i]);
            if (str1[i].equals("0")) {
                k = (2*k);
            }
            else {
                k = (2*k) + 1;
            }
        }
    }


    // OPT
    public int OPT(int set, int count1){
        int[] temp = new int[asc];
        int count = 0;
        for (int j=0; j<asc; j++){
            for (int i=count1-1; i<opt.size(); i++) {
                if (tag_index[set][j].equals(opt.get(i))) {
                    temp[j] = i;
                    count++;
                    break;
                    }
                }
            if (count == j) {
                return j;
            }
        }
        int max_index = 0;
        for (int k=0; k<asc; k++) {
            if (temp[k] > temp[max_index]) {
                max_index = k;
            }
        }
        return max_index;
    }
}
