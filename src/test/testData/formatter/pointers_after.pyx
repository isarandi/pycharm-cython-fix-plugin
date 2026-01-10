# Type declarations
ctypedef double *BB
ctypedef struct RLE:
    siz h
    uint *cnts
    uint *alloc

# Function with casts and address-of
def _to_leb128_dicts(RLEs Rs):
    cdef char *c_string
    for i in range(n):
        c_string = rleToString(<RLE *> &Rs._R[i])

def _from_leb128_dicts(rleObjs):
    rleFrString(<RLE *> &Rs._R[i], <const char *> c_string, h, w)

def frPoly(poly, siz h, siz w):
    rleFrPoly(<RLE *> &Rs._R[i], <const double *> np_poly.data, k, h, w)

cdef class RLECy:
    cdef RLE r
    def __dealloc__(self):
        rleFree(&self.r)
