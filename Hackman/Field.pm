package Hackman::Field;

use Hackman::Point;
use Hackman::Player;
use Moose;
use namespace::autoclean;

use constant {
    BUG     => 'E',
    SNIPPET => 'C',
    WEAPON  => 'W',
    PLAYER  => 'P',
};

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
                if ($c eq BUG) {
                    $arg->{bugs}{ $point->as_string } = $point;
                } elsif ($c eq WEAPON) {
                    $arg->{weapons}{ $point->as_string } = $point;
                } elsif ($c eq SNIPPET) {
                    $arg->{snippets}{ $point->as_string } = $point;
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
    is      => 'bare',
    isa     => 'HashRef',
    traits  => ['Hash'],
    handles => {
        walls   => 'values',
        is_wall => 'defined',
    },
    default => sub { {} },
);

has bugs => (
    is      => 'bare',
    isa     => 'HashRef[Hackman::Point]',
    traits  => ['Hash'],
    handles => {
        bugs    => 'values',
        has_bug => 'defined',
    },
    default => sub { {} },
);

has snippets => (
    is      => 'bare',
    isa     => 'HashRef[Hackman::Point]',
    traits  => ['Hash'],
    handles => {
        snippets    => 'values',
        has_snippet => 'defined',
    },
    default => sub { {} },
);

has weapons => (
    is      => 'bare',
    isa     => 'HashRef[Hackman::Point]',
    traits  => ['Hash'],
    handles => {
        weapons    => 'values',
        has_weapon => 'defined',
    },
    default => sub { {} },
);

has items => (
    is      => 'bare',
    isa     => 'HashRef[Hackman::Point]',
    traits  => ['Hash'],
    handles => {
        has_item => 'defined',
        nr_items => 'count',
    },
    builder => '_build_items',
    lazy    => 1,
);

sub _build_items {
    my ($self) = @_;

    my %items = map { $_->as_string => $_ } (
        $self->bugs,
        $self->snippets,
        $self->weapons,
    );
    return \%items;
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

    my $string = '    ';
    $string .= $_ < 10 || $_ % 2 ? sprintf('%2d', $_) : '  '
        for 0 .. $self->width - 1;
    $string .= "\n";

    $string .= '    ';
    $string .= '--' for 0 .. $self->width - 1;
    $string .= "-\n";

    for my $x (0 .. $self->height - 1) {
        $string .= sprintf '%2d |', $x;
        for my $y (0 .. $self->width - 1) {
            $string .= ' ' . $self->_cells->[$x][$y];
        }
        $string .= " |\n";
    }

    $string .= '    ';
    $string .= '--' for 0 .. $self->width - 1;
    $string .= "-\n";

    return $string;
}

__PACKAGE__->meta->make_immutable;
1;
