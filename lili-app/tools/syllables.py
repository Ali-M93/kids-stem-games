import unicodedata
SHORT='َُِ'; TAN='ًٌٍ'; SUK='ْ'; SHD='ّ'
def units(w):
    u=[]
    for ch in w:
        if unicodedata.category(ch)=='Mn' and u: u[-1]+=ch
        else: u.append(ch)
    return u
def syl(w):
    w=w.replace('ٰ','')
    U=units(w); out=[]; i=0
    while i<len(U):
        u=U[i]; base=u[0]; marks=u[1:]
        if base in ' ': return None
        if SHD in marks:
            # close previous with sukun copy
            if out: out[-1]+=base+SUK
            u=base+marks.replace(SHD,''); marks=u[1:]
        if any(m in marks for m in SHORT+TAN):
            s=u; v=[m for m in marks if m in SHORT][:1]
            # long vowel
            if i+1<len(U):
                n=U[i+1]
                if (n=='ا' and 'َ' in marks) or (n=='و' and 'ُ' in marks) or (n=='ي' and 'ِ' in marks) or n=='ى':
                    s+=n; i+=1
            # closing sukun consonant
            if i+1<len(U) and SUK in U[i+1] :
                s+=U[i+1]; i+=1
            elif i+1==len(U)-1 and not any(m in U[i+1][1:] for m in SHORT+TAN+SUK+SHD) and U[i+1][0] not in 'ة':
                s+=U[i+1]; i+=1   # bare last letter (pausal) closes syllable
            out.append(s)
        elif u=='ة' or base=='ة':
            if out: out[-1]+=u
        elif base=='ا' and not marks and out:
            out[-1]+=u
        elif base in 'ال' and not marks:
            out.append(u) if not out else out.__setitem__(-1,out[-1]+u)
        else:
            if out: out[-1]+=u
            else: out.append(u)
        i+=1
    return out
if __name__=='__main__':
    for w in ['فَارِسُ','رَسَمَ','مُوسَى','بَقَرَة','بُرْتُقَال','مِسْمَار','سَبُّورَة','بِطِّيخ','مَدْرَسَة','بَابَا','طَبِيب','دَفْتَر','حَلَزُون','يَسْتَيْقِظ','سَارَةُ','غَزَالُ','تَلَامِيذُهُ','مُنَبِّهُ','صَبَّاغُ','نَعَامَة']:
        print(w,syl(w))
