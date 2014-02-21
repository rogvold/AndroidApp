function penalty(r, r0){
    return (r[0] - r0[0])*(r[0] - r0[0]) + (r[1] - r0[1])*(r[1] - r0[1]);
}

function getTotalPenalty(points, r0){
    if (points == undefined){
        return undefined;
    }
    var s = 0;
    for (var i in points){
        s += penalty(points[i], r0);
    }
    return s;
}

function getMinimX(points){
    if (points == undefined){
        return undefined;
    }
    var minim = points[0][0];
    for (var i in points){
        if (points[i][0] < minim){
            minim = points[i][0];
        }
    }
    return minim;
}

function getMaximX(points){
    if (points == undefined){
        return undefined;
    }
    var maxim = points[0][0];
    for (var i in points){
        if (points[i][0] > maxim){
            maxim = points[i][0];
        }
    }
    return maxim;
}

function getEllipsePoint(r0, a, b, fi){
    var x = r0[0] + (1/Math.sqrt(2))*(a * Math.cos(fi) - b * Math.sin(fi));
    var y = r0[1] + (1/Math.sqrt(2))*(a * Math.cos(fi) + b * Math.sin(fi));
    return [x, y];
}

function generateEllipse(r0, a, b){
    var arr = new Array();
    var k = 100;
    var delta = 2*Math.PI / k;
    for (var i = 0; i < k; i++){
        var fi = i * delta;
        arr.push(getEllipsePoint(r0, a, b, fi));
    }
    return arr;
}


function pointIsInsideEllipse(r0, a, b, p){
    var x = p[0] - r0[0];
    var y = p[1] - r0[1];
    return  ((x + y)/a)*((x + y)/a) + ((x - y)/b)*((x - y)/b) < 2;
}

function inBounds(p){
    var min_bound = 270;
    var max_bound = 1500;
    if (p[0] < min_bound || p[0] > max_bound || p[1] < min_bound || p[1] > max_bound){
        return false;
    }
    return true;
}

function getPercentInside(r0, a, b, points){
    var k = 0;
    var m = 0;
    for (var i in points){
        if(!inBounds(points[i])){
            m++;
            continue;
        }
        if (pointIsInsideEllipse(r0, a, b, points[i])){
            k++;
        }
    }
    return 1.0*k/( points.length - m);
}

function getEllipseSquare(a, b){
    return Math.PI * a * b;
}

function findOptimum(r0, points){
    var a = 0;
    var b = 0;
    
    var opt = 0.95;
    
    var delta = 10;
    var n = 50;
    var maxP = 0;
    
    var cand = new Array();
    
    for (var i = 0; i < n; i++){
        for (var j = 0; j < n; j++){
            var insideP = getPercentInside(r0, i*delta, j*delta, points);
            if (insideP > opt){
                a = i*delta;
                b = j*delta;
                maxP = insideP;
                cand.push({
                    a: a,
                    b: b,
                    percent: insideP
                });
            }
        }
    }
    var min = 10*1000*1000;
    var res;
    for (var i in cand){
        if ( getEllipseSquare(cand[i].a, cand[i].b) < min ) {
            res = cand[i];
            min = getEllipseSquare(cand[i].a, cand[i].b);
        }
    }
    
    return [res.a, res.b];
}

function findCenter(points){
    x_step = 1;
    var mx = getMaximX(points);
    var mm = getMinimX(points);
    
    var minim = Math.floor(mm - (mx - mm) * 0.3);
    var maxim = Math.floor(mx + (mx - mm) * 0.3);
    var resX = minim;
    var minimPenalty = 1000000000;
    
    for (var x = minim; x <=maxim; x+=x_step){
        var p = getTotalPenalty(points, [x, x]);
        if (p < minimPenalty){
            minimPenalty = p;
            resX = x;
        }
    }
    return [resX, resX];
}