#include "main.h"

int MAXSIZE;

struct Node {
    char data;
    int frequency;
    int ord;
    Node* left;
    Node* right;

    Node(char d, int freq = 0, int ord = 0) : data(d), frequency(freq), ord(ord), left(nullptr), right(nullptr) {}
};

struct BSTNode {
    int val;
    int ord;
    BSTNode* left;
    BSTNode* right;

    BSTNode(int val, int ord) : val(val), ord(ord), left(nullptr), right(nullptr) {}
};

struct SNode {
    int arealabel;
    int val;
    int ord;
    queue<int> res;
};

Node* lastestCus = NULL;

void deleteHuffTree(Node* root) {
    if (root == NULL) return;
    deleteHuffTree(root->left);
    deleteHuffTree(root->right);
    delete root;
}

void deleteBST(BSTNode* root) {
    if (root == NULL) return;
    deleteBST(root->left);
    deleteBST(root->right);
    delete root;
}

class customer {
public:
    int val;
    customer* next;
public:
    customer() {}
    customer(int v, customer* ne) : val(v), next(ne) {}
    ~customer() {}
};

struct CompareNodes { // Comparison func for priority queue
    bool operator()(Node* const& node1, Node* const& node2) {
        if (node1->frequency == node2->frequency) {
            if (node1->data == node2->data)  return node1->ord > node2->ord;
            else if ((isupper(node1->data) && islower(node2->data)) || (islower(node1->data) && isupper(node2->data))) return isupper(node1->data);
            return node1->data > node2->data;
        }
        return node1->frequency > node2->frequency;
    }
};

int getHeight(Node* node)
{
    if (node == NULL) return 0;
    int lh = getHeight(node->left);
    int rh = getHeight(node->right);
    return (lh > rh ? lh : rh) + 1;
}

int getBalance(Node* node) {
    if (node == NULL) return 0;
    return getHeight(node->left) - getHeight(node->right);
}

Node* rotateRight(Node* root) {
    Node* newroot = root->left;
    root->left = newroot->right;
    newroot->right = root;
    return newroot;
}

Node* rotateLeft(Node* root) {
    Node* newroot = root->right;
    root->right = newroot->left;
    newroot->left = root;
    return newroot;
}

bool compare(pair<char, int>& a, pair<char, int>& b) {
    if (a.second == b.second) {
        if ((isupper(a.first) && isupper(b.first)) || (islower(a.first) && islower(b.first))) return a.first < b.first;
        return a.first > b.first;
    }
    return a.second < b.second;
}

Node* balanceTree(Node* node, int& balance_count, int& placement) {
    if (node == NULL) return node;
    int balance = getBalance(node);
    if (balance >= -1 && balance <= 1) return node;
    // Left Left
    if (balance > 1 && getBalance(node->left) >= 0) {
        balance_count++;
        placement++;
        return rotateRight(node);
    }
    // Right Right
    if (balance < -1 && getBalance(node->right) <= 0) {
        balance_count++;
        placement++;
        return rotateLeft(node);
    }
    // Left Right
    if (balance > 1 && getBalance(node->left) < 0) {
        balance_count++;
        placement++;
        node->left = rotateLeft(node->left);
        return rotateRight(node);
    }
    // Right Left
    if (balance < -1 && getBalance(node->right) > 0) {
        balance_count++;
        placement++;
        node->right = rotateRight(node->right);
        return rotateLeft(node);
    }
    return node;
}

Node* traverseBalance(Node* root, int& balance_count, int& placement, bool& rotated) {
    if (balance_count >= 3 || root == NULL) return root;
    int balance = getBalance(root);
    if (balance < -1 || balance > 1) {
        root = balanceTree(root, balance_count, placement);
        root->ord = placement;
        rotated = true;
        return root;
    }
    root->left = traverseBalance(root->left, balance_count, placement, rotated);
    root->right = traverseBalance(root->right, balance_count, placement, rotated);
    return root;
}

map<char, int> caesarCipherAndAccumulate(string name, map<char,int> charFrequency, string& cname) {
    map<char, int> newfreq;
    for (char ch : name) {
        if (isalpha(ch)) {
            char base = isupper(ch) ? 'A' : 'a';
            char encryptedCh = (ch - base + charFrequency[ch]) % 26 + base;
            cname += encryptedCh;
            newfreq[encryptedCh]++;
        }
    }
    return newfreq;
}

int binaryToDecimal(string binaryStr) {
    int res = 0;
    int power = 1;
    for (int i = binaryStr.length() - 1; i >= 0; i--) {
        if (binaryStr[i] == '1') {
            res += power;
        }
        power *= 2;
    }
    return res;
}

class UnlimitedVoidres {
public:
    unordered_map<int, BSTNode*> hash;
    int time = 0;

    ~UnlimitedVoidres() {
        for (int i = 1; i <= MAXSIZE; i++) {
            if(hash[i]) deleteBST(hash[i]);
        }
    }

    BSTNode* insertBSTNode(BSTNode* root, int res) {
        if (root == nullptr) return new BSTNode(res, time);
        if (res < root->val) root->left = insertBSTNode(root->left, res);
        else if (res >= root->val) root->right = insertBSTNode(root->right, res);
        return root;
    }

    void addCusG(int result) {
        int ID = result % MAXSIZE + 1;
        BSTNode* rootG = hash[ID];
        time++;
        hash[ID] = insertBSTNode(rootG, result);
    }

    void findFirstCus(BSTNode* root, int& value, int& mintime) {
        if (root == NULL) return;
        if (root->ord < mintime) {
            mintime = root->ord;
            value = root->val;
        }
        findFirstCus(root->left, value, mintime);
        findFirstCus(root->right, value, mintime);
    }

    BSTNode* deleteBSTNode(BSTNode* root, int val) {
        if (root == NULL) return root;
        if (root->val > val) {
            root->left = deleteBSTNode(root->left, val);
            return root;
        }
        else if (root->val < val) {
            root->right = deleteBSTNode(root->right, val);
            return root;
        }
        if (!root->left) {
            BSTNode* temp = root->right;
            root->right = NULL;
            delete root;
            return temp;
        }
        else if (!root->right) {
            BSTNode* temp = root->left;
            root->left = NULL;
            delete root;
            return temp;
        }
        else {
            BSTNode* succParent = root;
            BSTNode* succ = root->right;
            while (succ->left) {
                succParent = succ;
                succ = succ->left;
            }
            if (succParent != root) succParent->left = succ->right;
            else succParent->right = succ->right;
            root->val = succ->val;
            root->ord = succ->ord;
            succ->left = NULL;
            succ->right = NULL;
            delete succ;
            return root;
        }
    }

    void removeCusG(BSTNode*& root) {
        if (root == NULL) return;
        BSTNode* temp = root;
        int value = temp->val;
        int mintime = temp->ord;
        
        findFirstCus(temp, value, mintime);
        root = deleteBSTNode(root, value);

    }

    void getPreOrder(BSTNode* root, vector<int>& preOrd) {
        if (root == NULL) return;
        preOrd.push_back(root->val);
        getPreOrder(root->left, preOrd);
        getPreOrder(root->right, preOrd);
    }

    int getNumCustomer(BSTNode* root) {
        if (root == NULL) return 0;
        return 1 + getNumCustomer(root->left) + getNumCustomer(root->right);
    }

    void printInOrder(BSTNode* root) {
        if (root == NULL) return;
        printInOrder(root->left);
        cout << root->val << endl;
        printInOrder(root->right);
    }
};

UnlimitedVoidres Gres;

class MalevolentShrineres {
public:
    vector<SNode> minHeap;
    int time = 0;
    void addCusS(int res) {
        int ID = res % MAXSIZE + 1;
        bool foundArea = false;
        if (minHeap.empty()) addArea(ID);
        else {
            int size = minHeap.size();
            for (int i = 0; i < size; i++) {
                if (minHeap[i].arealabel == ID) foundArea = true;
            }
            if (foundArea == false) addArea(ID);
        }
        addCusAndreHeap(ID, res);
    }

    void reheapUP(int idx) {
        while (idx > 0) {
            int parent = floor((idx - 1) / 2);
            if (minHeap[idx].val == minHeap[parent].val && minHeap[idx].ord < minHeap[parent].ord) {
                swap(minHeap[idx], minHeap[parent]);
                idx = parent;
            }
            if (minHeap[idx].val < minHeap[parent].val) {
                swap(minHeap[idx], minHeap[parent]);
                idx = parent;
            }
            else break;
        }
    }

    void reheapDown(int idx) {
        if (minHeap.size() == 1) return;
        int leftChild = idx * 2 + 1;
        int rightChild = idx * 2 + 2;
        int smallest = idx;
        int size = minHeap.size();
        if (leftChild < size) {
            if (minHeap[leftChild].val <= minHeap[smallest].val) {
                if (minHeap[leftChild].val == minHeap[smallest].val && minHeap[leftChild].ord < minHeap[smallest].ord) {
                    smallest = leftChild;
                }
                else if (minHeap[leftChild].val < minHeap[smallest].val)
                    smallest = leftChild;
            }
        }
        if (rightChild < size) {
            if (minHeap[rightChild].val <= minHeap[smallest].val) {
                if (minHeap[rightChild].val == minHeap[smallest].val && minHeap[rightChild].ord < minHeap[smallest].ord) {
                    smallest = rightChild;
                }
                else if (minHeap[rightChild].val < minHeap[smallest].val)
                    smallest = rightChild;
            }
        }
        if (idx != smallest) {
            swap(minHeap[idx], minHeap[smallest]);
            reheapDown(smallest);
        }
    }

    void addArea(int label) {
        SNode newarea = { label, 0 };
        minHeap.push_back(newarea);
        reheapUP(minHeap.size() - 1);
    }

    void addCusAndreHeap(int label, int res) {
        int size = minHeap.size();
        for (int i = 0; i < size; i++) {
            if (label == minHeap[i].arealabel) {
                minHeap[i].val++;
                time++;
                minHeap[i].ord = time;
                minHeap[i].res.push(res);
                reheapDown(i);
                reheapUP(i);
                break;
            }
        }
    }

    void deleteArea(int idx) {
        int size = minHeap.size();
        if (size == 0) return;
        if (size == 1) {
            minHeap.erase(minHeap.begin());
        }
        else {
            if (idx != size - 1 && size > 1) {
                swap(minHeap[idx], minHeap[size - 1]);
            }
            minHeap.pop_back();
            reheapDown(idx);
            reheapUP(idx);
        }
    }

    void printcusS() {
        cout << "----------SUKUNA----------" << endl;
        int size = minHeap.size();
        for (int i = 0; i < size; i++) {
            cout << minHeap[i].arealabel << "-" << minHeap[i].val << ": ";
            printqueue(minHeap[i].res);
        }
        cout << "--------------------------" << endl;
    }
    
    void printqueue(queue<int> q) {
        while (!q.empty()) {
            cout << q.front() << " ";
            q.pop();
        }
        cout << endl;
    }

    void printInOrderArea(int i, int num) {
        int size = minHeap.size();
        if (i >= size) return;
        stack<int> s;
        int c = num;
        queue<int> q = minHeap[i].res;
        while (!q.empty()) {
            int node = q.front();
            q.pop();
            s.push(node);
        }
        while (!s.empty() && c != 0) {
            cout << minHeap[i].arealabel << "-" << s.top() << endl;
            s.pop();
            c--;
        }
        printInOrderArea(i * 2 + 1,num);
        printInOrderArea(i * 2 + 2,num);
    }
};

MalevolentShrineres Sres;

long long DFS(vector<int>& arr, vector<vector<long long>>& table) {
    int size = arr.size();
    if (size <= 2) return 1;
    vector<int> leftSubTree;
    vector<int> rightSubTree;
    for (int i = 1; i < size; ++i) {
        if (arr[i] < arr[0]) leftSubTree.push_back(arr[i]);
        else rightSubTree.push_back(arr[i]);
    }
    long long countLeft = DFS(leftSubTree, table);
    long long countRight = DFS(rightSubTree, table);
    return (table[size - 1][leftSubTree.size()] * (((countLeft * countRight) % MAXSIZE))) % MAXSIZE;
}

int countPermu(vector<int>& arr) {
    int size = arr.size();
    vector<vector<long long>> table;
    table.resize(size + 1);
    for (int i = 0; i < size + 1; ++i) {
        table[i] = vector<long long>(i + 1, 1);
        for (int j = 1; j < i; ++j) {
            table[i][j] = (table[i - 1][j - 1] + table[i - 1][j]) % MAXSIZE;
        }
    }
    return DFS(arr, table);
}

void getLetterCodes(Node* root, string str, unordered_map <char, string>& map) {
    if (root == NULL) return;
    if (root->data != '~') map[root->data] = str;
    getLetterCodes(root->left, str + "0", map);
    getLetterCodes(root->right, str + "1", map);
}

bool compareArea(SNode& a, SNode& b) {
    if (a.val < b.val) return true;
    else if (a.val == b.val && a.ord < b.ord) return true;
    else return false;
}

void printLatest(Node* root) {
    if (root == NULL) return;
    printLatest(root->left);
    if (root->data == '~') cout << root->frequency << endl;
    else cout << root->data << endl;
    printLatest(root->right);
}

void LAPSE(string name) {
    map<char, int> charFrequency;
    for (char ch : name) {
        charFrequency[ch]++;
    }
    if (charFrequency.size() < 3) return;
    string caesarName = "";
    charFrequency = caesarCipherAndAccumulate(name, charFrequency, caesarName);
    
    vector<pair<char, int>> charFrequencyList(charFrequency.begin(), charFrequency.end());
    sort(charFrequencyList.begin(), charFrequencyList.end(), compare);

    priority_queue <Node*, vector<Node*>, CompareNodes> minHeap;
    int placement = 0;
    for (const auto entry : charFrequencyList) {
        minHeap.push(new Node(entry.first, entry.second, placement));
        placement++;
    }
    while (minHeap.size() > 1) {
        Node* left = minHeap.top();
        minHeap.pop();
        Node* right = minHeap.top();
        minHeap.pop();

        Node* internalNode = new Node('~', left->frequency + right->frequency, placement);
        placement++;
        internalNode->left = left;
        internalNode->right = right;
        int balance_count = 0;
        bool rotated = false;
        do {
            rotated = false;
            internalNode = traverseBalance(internalNode, balance_count, placement, rotated);
        } while (rotated == true && balance_count < 3);
        minHeap.push(internalNode);
    }
    Node* huffmanTreeRoot = minHeap.top();
    if (huffmanTreeRoot->data != '~' && getHeight(huffmanTreeRoot) > 1) {
        deleteHuffTree(huffmanTreeRoot);
        return;
    }
    if (lastestCus != NULL) {
        deleteHuffTree(lastestCus);
    }
    lastestCus = huffmanTreeRoot;
    string binaryStr = "";
    unordered_map<char, string> lettercode;
    getLetterCodes(huffmanTreeRoot, "", lettercode);
    for (char n : caesarName) binaryStr += lettercode[n];
    reverse(binaryStr.begin(), binaryStr.end());
    string subbin = binaryStr.substr(0, 10);

    int result = binaryToDecimal(subbin);
    if (result % 2 == 1) {
        Gres.addCusG(result);
    }
    else {
        Sres.addCusS(result);
    }
}

void KOKUSEN() {
    for (int i = 1; i <= MAXSIZE; i++) {
        vector<int> preOrder;
        Gres.getPreOrder(Gres.hash[i], preOrder);
        int cus = Gres.getNumCustomer(Gres.hash[i]);
        int Y = countPermu(preOrder);
        if (Y < cus) {
            for (int j = 0; j < Y; j++) {
                Gres.removeCusG(Gres.hash[i]);
            }
        }
        else {   
            deleteBST(Gres.hash[i]);
            Gres.hash[i] = NULL;
        }
    }
}

void KEITEIKEN(int num) {
    if (num <= 0) return;
    else if (num > MAXSIZE) num = MAXSIZE;
    int size = Sres.minHeap.size();
    if (size == 0) return;
    vector<SNode> delord;
    for (int i = 0; i < size; i++) {
        delord.push_back(Sres.minHeap[i]);
    }

    sort(delord.begin(), delord.end(), compareArea);

    for (int i = 0; i < num; i++) {
        int cus = num; 
        if (delord.empty()) break;
        int label = delord.front().arealabel;
        int minidx = 0;
        for (int j = 0; j < size; j++) {
            if (Sres.minHeap[j].arealabel == label) {
                minidx = j;
                break;
            }
        }
        delord.erase(delord.begin());
        Sres.time++;
        Sres.minHeap[minidx].ord = Sres.time;
        while (cus != 0 && !Sres.minHeap[minidx].res.empty()) {
            cout << Sres.minHeap[minidx].res.front() << "-" << Sres.minHeap[minidx].arealabel << endl;
            Sres.minHeap[minidx].res.pop();
            Sres.minHeap[minidx].val--;
            cus--;
        }
        if (Sres.minHeap[minidx].res.empty()) Sres.deleteArea(minidx);
        else {
            Sres.reheapUP(minidx);
            Sres.reheapDown(minidx);
        }
    }
}

void HAND() {
    if (lastestCus == NULL) return;
    printLatest(lastestCus);
}

void LIMITLESS(int num) {
    if (num < 1 || num > MAXSIZE) return;
    if (Gres.hash[num] == NULL) return;
    Gres.printInOrder(Gres.hash[num]);
}

void CLEAVE(int num){
    Sres.printInOrderArea(0, num);
}

void simulate(string filename)
{
    ifstream ss(filename);
    string str, maxsize, name, num;
    while (ss >> str)
    {
        if (str == "MAXSIZE")
        {
            ss >> maxsize;
            MAXSIZE = stoi(maxsize);
        }
        else if (str == "LAPSE") // LAPSE <NAME>
        {
            ss >> name;
            LAPSE(name);
        }
        else if (str == "KOKUSEN") // KOKUSEN
        {
            KOKUSEN();
        } 
        else if (str == "KEITEIKEN") // KEITEIKEN <NUM>
        {
            ss >> num;
            int n = stoi(num);
            KEITEIKEN(n);
        }         
        else if (str == "HAND") // HAND
        {
            HAND();
        }
        else if (str == "LIMITLESS") // LIMITLESS <NUM>
        {
            ss >> num;
            int n = stoi(num);
            LIMITLESS(n);
        }
        else if (str == "CLEAVE") // CLEAVE <NUM>
        {
            ss >> num;
            int n = stoi(num);
            CLEAVE(n);
        }
    }
	return;
}