package Hackman::Field;

use Hackman::Point;
use Hackman::Player;
use Moose;
use namespace::autoclean;

around BUILDARGS => sub {
    my $orig  = shift;
    my $class = shift;
    my $arg   = $class->$orig(@_);

    # $arg->{bugs}     = [];
	# $arg->{weapons}  = [];
	# $arg->{snippets} = [];

    my @c = split ',', $arg->{cells};
    my @cells;
    for my $x (0 .. $arg->{height} - 1) {
        for my $y (0 .. $arg->{width} - 1) {
            my $cell = shift @c;
            $cells[$x][$y] = $cell;

            next if $cell eq '.';

            if ($cell eq 'x') {
                $arg->{walls}{"$x,$y"} = 1;
                next;
            }

            my $point = Hackman::Point->new(x => $x, y => $y);
            for my $c (split '', $cell) {
                if ($c eq 'E') {
					push @{ $arg->{bugs} }, $point;
				} elsif ($c eq 'W') {
					push @{ $arg->{weapons} }, $point;
				} elsif ($c eq 'C') {
					push @{ $arg->{snippets} }, $point;
                } else {
                    my $player_id = ord($c) - ord('0');
                    push @{ $arg->{players} },
                        Hackman::Player->new(id => $player_id, point => $point);
                }
            }
        }
    }
    $arg->{cells} = \@cells;

    return $arg;
};

has height => (
    is       => 'ro',
    isa      => 'Int',
    required => 1,
);

has width => (
    is       => 'ro',
    isa      => 'Int',
    required => 1,
);

has cells => (
    is       => 'bare',
    reader   => '_cells',
    isa      => 'ArrayRef',
    required => 1,
);

has players => (
    is       => 'bare',
    isa      => 'ArrayRef',
    traits   => ['Array'],
    handles  => { players => 'elements' },
    required => 1,
);

# --------------------------------

has walls => (
    is      => 'ro',
    isa     => 'HashRef',
    default => sub { {} },
);

has bugs => (
    is      => 'ro',
    isa     => 'ArrayRef[Hackman::Point]',
    default => sub { [] },
);

has snippets => (
    is      => 'ro',
    isa     => 'ArrayRef[Hackman::Point]',
    default => sub { [] },
);

has weapons => (
    is      => 'ro',
    isa     => 'ArrayRef[Hackman::Point]',
    default => sub { [] },
);

sub is_wall {
    my ($self, $point) = @_;
    return $self->walls->{ $point->as_string };
}

sub is_valid {
    my ($self, $point) = @_;

    my $x = $point->x;
    my $y = $point->y;

    return 0 <= $x && $x < $self->height
        && 0 <= $y && $y < $self->width;
}

sub as_string {
    my ($self) = @_;

    my $string = '';
    for my $x (0 .. $self->height - 1) {
        for my $y (0 .. $self->width - 1) {
            $string .= $self->_cells->[$x][$y];
        }
        $string .= "\n";
    }
    return $string;
}

__PACKAGE__->meta->make_immutable;
1;
